/*
 * Copyright (C) 2025  rob at applecommander.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.applecommander.disassembler.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.stream.Collectors;

import org.applecommander.disassembler.api.Disassembler;
import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.pcode.InstructionSetPCode;
import org.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;
import org.applecommander.disassembler.api.switching6502.InstructionSet6502Switching;
import org.applecommander.disassembler.api.z80.InstructionSetZ80;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "acdasm", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n",
         description = "AC Disassembler.")
public class Main implements Callable<Integer> {
    @Option(names = "--debug", description = "Print stack traces")
    public static boolean debug;

    @Option(names = { "-a", "--addr", "--origin" }, converter = IntegerTypeConverter.class,
            description = "Set start address for application.")
    private int startAddress = -1;
    
    @Option(names = { "--offset" }, converter = IntegerTypeConverter.class, 
            description = "Skip offset bytes into binary before disassembling.")
    private int offset;

    @Option(names = { "-n", "--length" }, converter = IntegerTypeConverter.class,
            description = "Disassembly length bytes.")
    private int length;
    
    @Option(names = { "--labels" }, negatable = true, description = "Show or hide labels.")
    public void selectLabelEmitter(boolean flag) {
        emitter = flag ? this::emitWithLabels : this::emitRaw;
    }
    private Consumer<Instruction> emitter = this::emitWithLabels;
    
    @Option(names = { "-l", "--library" }, split = ",", paramLabel = "<library>", description =
            "Select which library labels to load. Use 'All' to select all. Each CPU has a default set " +
            "(most are 'All' except Z80).  Options are: 'F800', 'Applesoft', 'ProDOS', 'DOS', 'DISKII'. " +
            "'None' may also be used to turn library labels off.")
    private List<String> libraries;

    @ArgGroup(heading = "%nCPU Selection:%n")
    private final CpuSelection cpuSelection = new CpuSelection();
    
    @Parameters(arity = "1", description = "File to disassemble.")
    private Path file;

    // Locals
    private final Map<Integer,String> labels = new HashMap<>();
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main())
                           .setExecutionExceptionHandler(new PrintExceptionMessageHandler())
                           .execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        final int MAX_ADDRESS = 0xFFFF;

        if (startAddress == -1) {
            startAddress = cpuSelection.instructionSet.defaultStartAddress();
        }

        if (startAddress < 0 || startAddress > MAX_ADDRESS) {
            String errormsg = String.format("start address(%d) is out of range(0-%d).", startAddress, MAX_ADDRESS);
            throw new IllegalArgumentException(errormsg);
        }

        final byte[] code = Files.readAllBytes(file);
        if (length == 0) {
            length = code.length;
        }

        if (offset < 0 || offset > code.length) {
            String errormsg = String.format("offset(%d) is out of range(0-%d).", offset, code.length);
            throw new IllegalArgumentException(errormsg);
        }

        // CPU library labels defaults:
        if (libraries == null) {
            libraries = cpuSelection.instructionSet.defaultLibraryLabels();
        }
        // Remap the keywords:  (note: Most libraries will be defined with "List.of('All|None')" which is immutable)
        if (libraries.contains("All")) {
            libraries = new ArrayList<>();
            libraries.addAll(Disassembler.sections());
        }
        else if (libraries.contains("None")) {
            libraries = new ArrayList<>();
        }

        List<Instruction> assembly = Disassembler.with(code)
                .startingAddress(startAddress)
                .bytesToSkip(offset)
                .bytesToDecode(length)
                .use(cpuSelection.get())
                .section(libraries)
                .decode(labels);

        assembly.forEach(emitter);
        
        return 0;
    }
    
    public void emitWithLabels(Instruction instruction) {
        int bytesPerLine = cpuSelection.instructionSet.suggestedBytesPerInstruction();
        System.out.printf("%04X- ", instruction.address());
        
        byte[] code = instruction.code();
        for (int i=0; i<bytesPerLine; i++) {
            if (i >= code.length) {
                System.out.print("   ");
            } else {
                System.out.printf("%02X ", code[i]);
            }
        }
        System.out.printf(" %-10.10s ", labels.getOrDefault(instruction.address(), ""));
        System.out.printf("%-5.5s ", instruction.mnemonic());
        System.out.printf("%s\n", instruction.operands().stream().map(operand -> {
                if (operand.address().isPresent() && labels.containsKey(operand.address().get())) {
                    return operand.format(labels.get(operand.address().get()));
                }
                else {
                    return operand.format();
                }
            })
            .collect(Collectors.joining(",")));

        if (code.length >= bytesPerLine) {
            for (int i=bytesPerLine; i<code.length; i++) {
                if (i % bytesPerLine == 0) {
                    if (i > bytesPerLine) System.out.println();
                    System.out.printf("%04X- ", instruction.address()+i);
                }
                System.out.printf("%02X ", code[i]);
            }
            System.out.println();
        }
    }
    public void emitRaw(Instruction instruction) {
        int bytesPerLine = cpuSelection.instructionSet.suggestedBytesPerInstruction();
        System.out.printf("%04X- ", instruction.address());
        
        byte[] code = instruction.code();
        for (int i=0; i<bytesPerLine; i++) {
            if (i >= code.length) {
                System.out.print("   ");
            } else {
                System.out.printf("%02X ", code[i]);
            }
        }
        System.out.printf(" %-5.5s ", instruction.mnemonic());
        System.out.printf("%s\n", instruction.operands().stream().map(Instruction.Operand::format)
                .collect(Collectors.joining(",")));

        if (code.length >= bytesPerLine) {
            for (int i=bytesPerLine; i<code.length; i++) {
                if (i % bytesPerLine == 0) {
                    if (i > bytesPerLine) System.out.println();
                    System.out.printf("%04X- ", instruction.address()+i);
                }
                System.out.printf("%02X ", code[i]);
            }
            System.out.println();
        }
    }
    
    private static class CpuSelection {
        private InstructionSet instructionSet = InstructionSet6502.for6502();
        
        public InstructionSet get() {
            return this.instructionSet;
        }
        
        @Option(names = { "--6502" }, description = "MOS 6502.")
        public void select6502(boolean flag) {
            this.instructionSet = InstructionSet6502.for6502();
        }
        @Option(names = { "--65c02", "--65C02" }, description = "WDC 65C02.")
        public void select65C02(boolean flag) {
            this.instructionSet = InstructionSet6502.for65C02();
        }
        @Option(names = { "--6502x", "--6502X" }, description = "MOS 6502 + 'illegal' instructions.")
        public void select6502X(boolean flag) {
            this.instructionSet = InstructionSet6502.for6502withIllegalInstructions();
        }
        @Option(names = { "--sweet16", "--SWEET16" }, description = "SWEET16.")
        public void selectSWEET16(boolean flag) {
            this.instructionSet = InstructionSetSWEET16.forSWEET16();
        }
        @Option(names = { "--6502s", "--6502S" }, description = "MOS 6502 with SWEET16 switching.")
        public void select6502Switching(boolean flag) {
            this.instructionSet = InstructionSet6502Switching.withSwitching();
        }
        @Option(names = { "--z80", "--Z80" }, description = "Zilog Z80.")
        public void selectZ80(boolean flag) {
            this.instructionSet = InstructionSetZ80.forZ80();
        }
        @Option(names = { "--pcode", "--PCODE" }, description = "Apple Pascal p-code")
        public void selectPCODE(boolean flag) {
            this.instructionSet = InstructionSetPCode.forApplePascal();
        }
    }
}
