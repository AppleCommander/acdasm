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
package io.github.applecommander.disassembler.cli;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

import io.github.applecommander.disassembler.api.Disassembler;
import io.github.applecommander.disassembler.api.Instruction;
import io.github.applecommander.disassembler.api.InstructionSet;
import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;
import io.github.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;
import io.github.applecommander.disassembler.api.switching6502.InstructionSet6502Switching;
import io.github.applecommander.disassembler.api.z80.InstructionSetZ80;
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
    @Option(names = { "-a", "--addr", "--origin" }, converter = IntegerTypeConverter.class,
            description = "Set start address for application.")
    private int startAddress = -1;
    
    @Option(names = { "--offset" }, converter = IntegerTypeConverter.class, 
            description = "Skip offset bytes into binary before disassembling.")
    private int offset;
    
    @Option(names = { "--hide-labels" }, negatable = true, description = "Hide labels.")
    public void selectLabelEmitter(boolean flag) {
        emitter = flag ? this::emitWithLabels : this::emitRaw;
    }
    private Consumer<Instruction> emitter = this::emitWithLabels;
    
    @Option(names = { "--labels" }, split = ",", defaultValue = "All", description = 
            "Select which library labels to load (default = 'All'; options are 'F800', 'Applesoft', 'ProDOS', 'DOS33', 'None').")
    private List<String> labels;

    @ArgGroup(heading = "%nCPU Selection:%n")
    private CpuSelection cpuSelection = new CpuSelection();
    
    @Parameters(arity = "1", description = "File to disassemble.")
    private Path file;
    
    public static void main(String[] args) {
        int exitCode = new CommandLine(new Main())
                           .setExecutionExceptionHandler(new PrintExceptionMessageHandler())
                           .execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
        final int MAX_ADDRESS = 0xFFFF;
        final int MAX_OFFSET = 0xFFFF;

        if (startAddress == -1) {
            startAddress = cpuSelection.instructionSet.defaultStartAddress();
        }

        if (startAddress < 0 || startAddress > MAX_ADDRESS) {
            String errormsg = String.format("start address(%d) is out of range(0-%d).", startAddress, MAX_ADDRESS);
            throw new IllegalArgumentException(errormsg);
        }

        if (offset < 0 || offset > MAX_OFFSET) {
            String errormsg = String.format("offset(%d) is out of range(0-%d).", offset, MAX_OFFSET);
            throw new IllegalArgumentException(errormsg);
        }

        final byte[] code = Files.readAllBytes(file);
        
        if (labels.contains("All")) {
            labels.clear();
            labels.addAll(Disassembler.sections());
        }
        else if (labels.contains("None")) {
            labels.clear();
        }
        
        List<Instruction> instructions = Disassembler.with(code)
                .startingAddress(startAddress)
                .bytesToSkip(offset)
                .use(cpuSelection.get())
                .section(labels)
                .decode();

        instructions.forEach(emitter::accept);
        
        return 0;
    }
    
    public void emitWithLabels(Instruction instruction) {
        System.out.printf("%04X- ", instruction.getAddress());
        
        byte[] code = instruction.getBytes();
        for (int i=0; i<3; i++) {
            if (i >= code.length) {
                System.out.printf("   ");
            } else {
                System.out.printf("%02X ", code[i]);
            }
        }
        System.out.printf(" %-10.10s ", instruction.getAddressLabel().orElse(""));
        System.out.printf("%s\n", instruction.formatOperandWithLabel());
    }
    public void emitRaw(Instruction instruction) {
        System.out.printf("%04X- ", instruction.getAddress());
        
        byte[] code = instruction.getBytes();
        for (int i=0; i<3; i++) {
            if (i >= code.length) {
                System.out.printf("   ");
            } else {
                System.out.printf("%02X ", code[i]);
            }
        }
        System.out.printf(" %s\n", instruction.formatOperandWithValue());
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
        @Option(names = { "--65C02" }, description = "WDC 65C02.")
        public void select65C02(boolean flag) {
            this.instructionSet = InstructionSet6502.for65C02();
        }
        @Option(names = { "--6502X" }, description = "MOS 6502 + 'illegal' instructions.")
        public void select6502X(boolean flag) {
            this.instructionSet = InstructionSet6502.for6502withIllegalInstructions();
        }
        @Option(names = { "--SWEET16" }, description = "SWEET16.")
        public void selectSWEET16(boolean flag) {
            this.instructionSet = InstructionSetSWEET16.forSWEET16();
        }
        @Option(names = { "--6502S" }, description = "MOS 6502 with SWEET16 switching.")
        public void select6502Switching(boolean flag) {
            this.instructionSet = InstructionSet6502Switching.withSwitching();
        }
        @Option(names = { "--Z80" }, description = "Zilog Z80.")
        public void selectZ80(boolean flag) {
            this.instructionSet = new InstructionSetZ80();
        }
    }
}
