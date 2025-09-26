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

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.applecommander.disassembler.api.Disassembler;
import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.Program;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.pcode.InstructionSetPCode;
import org.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;
import org.applecommander.disassembler.api.switching6502.InstructionSet6502Switching;
import org.applecommander.disassembler.api.z80.InstructionSetZ80;
import org.applecommander.disassembler.cli.codefile.AssemblyProcedure;
import org.applecommander.disassembler.cli.codefile.CodeFile;
import org.applecommander.disassembler.cli.codefile.PCodeProcedure;
import org.applecommander.disassembler.cli.codefile.Segment;
import picocli.CommandLine;
import picocli.CommandLine.ArgGroup;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Help.*;

import static picocli.CommandLine.Model.UsageMessageSpec.*;

@Command(name = "acdasm", mixinStandardHelpOptions = true, versionProvider = VersionProvider.class,
         descriptionHeading = "%n",
         optionListHeading = "%nOptions:%n",
         description = "AppleCommander Disassembler.%n")
public class Main implements Callable<Integer> {
    @Option(names = "--debug", description = "Print stack traces")
    public static boolean debug;

    @Option(names = { "-a", "--addr", "--origin" }, converter = IntegerTypeConverter.class,
            description = "Set start address for application.")
    private Integer startAddress;
    
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

    @Option(names = { "--descriptions" }, negatable = true, description = "Include opcode descriptions.")
    private Boolean descriptions;
    
    @Parameters(arity = "1", description = "File to disassemble.")
    private Path file;

    // Locals
    private final Map<Integer,String> labels = new HashMap<>();
    
    public static void main(String[] args) {
        CommandLine cl = new CommandLine(new Main());
        cl.getHelpSectionMap().put(SECTION_KEY_FOOTER_HEADING,
                help -> help.createHeading("%nProcessor Defaults:%n"));
        cl.getHelpSectionMap().put(SECTION_KEY_FOOTER, help -> {
                    TextTable table = TableBuilder.with(help)
                            .textHeader("Default Value")
                            .textHeader("6502", InstructionSet6502.for6502())
                            .textHeader("6502X", InstructionSet6502.for6502withIllegalInstructions())
                            .textHeader("6502S", InstructionSet6502Switching.withSwitching())
                            .textHeader("65C02", InstructionSet6502.for65C02())
                            .textHeader("SWEET-16", InstructionSetSWEET16.forSWEET16())
                            .textHeader("Z80", InstructionSetZ80.forZ80())
                            .textHeader("P-CODE", InstructionSetPCode.forApplePascal())
                            .row("Start Address", set -> {
                                if (set instanceof InstructionSetZ80) {
                                    return String.format("%04xH", set.defaults().startAddress());
                                }
                                return String.format("$%04X", set.defaults().startAddress());
                            })
                            .row("Library Labels", set -> {
                                if (set.defaults().libraryLabels().isEmpty()) {
                                    return "None";
                                }
                                return String.join(",", set.defaults().libraryLabels());
                            })
                            .row("Bytes/Instruction", set -> String.format("%d", set.defaults().bytesPerInstruction()))
                            .row("Descriptions?", set -> set.defaults().includeDescription() ? "No" : "Yes")
                            .build();
                    return table.toString();
                });
        cl.setExecutionExceptionHandler(new PrintExceptionMessageHandler());

        int exitCode = cl.execute(args);
        System.exit(exitCode);
    }

    @Override
    public Integer call() throws Exception {
        final int MAX_ADDRESS = 0xFFFF;

        if (startAddress == null) {
            startAddress = cpuSelection.instructionSet.defaults().startAddress();
        }

        if (startAddress < 0 || startAddress > MAX_ADDRESS) {
            String errormsg = String.format("start address(%d) is out of range(0-%d).", startAddress, MAX_ADDRESS);
            throw new IllegalArgumentException(errormsg);
        }

        final byte[] code = Files.readAllBytes(file);

        if (offset < 0 || offset > code.length) {
            String errormsg = String.format("offset(%d) is out of range(0-%d).", offset, code.length);
            throw new IllegalArgumentException(errormsg);
        }

        // CPU library labels defaults:
        if (libraries == null) {
            libraries = cpuSelection.instructionSet.defaults().libraryLabels();
        }
        // Remap the keywords:  (note: Most libraries will be defined with "List.of('All|None')" which is immutable)
        if (libraries.contains("All")) {
            libraries = new ArrayList<>();
            libraries.addAll(Disassembler.sections());
        }
        else if (libraries.contains("None")) {
            libraries = new ArrayList<>();
        }

        if (descriptions == null) {
            descriptions = cpuSelection.instructionSet.defaults().includeDescription();
        }

        switch (this.cpuSelection.type) {
            case ASSEMBLY -> disassemble(code);
            case CODEFILE -> disassemble(CodeFile.load(code));
        }

        return 0;
    }

    public void disassemble(byte[] code) {
        List<Instruction> assembly = Disassembler.with(code)
                .startingAddress(startAddress)
                .bytesToSkip(offset)
                .bytesToDecode(length)
                .use(cpuSelection.get())
                .section(libraries)
                .decode(labels);

        assembly.forEach(emitter);
    }

    public void disassemble(CodeFile codeFile) {
        // Reset components that are implied in the CodeFile itself:
        offset = 0;
        length = 0;
        if (codeFile.comment() != null && !codeFile.comment().isEmpty()) {
            System.out.printf("Comment:  %s\n", codeFile.comment());
        }
        for (Segment segment : codeFile.segments()) {
            if (segment != null) disassemble(segment);
        }
    }

    public void disassemble(Segment segment) {
        System.out.printf(">> Seg #%02d: FROM=$%04x, TO=$%04x, N='%s', %-10s, T=$%04x, M=%-10s, Ver=%d\n",
                segment.segNum(), segment.data().position(), segment.data().limit(), segment.name(),
                segment.kind(), segment.textAddr(), segment.machineType(), segment.version());
        if (segment.textInterface() != null && !segment.textInterface().isEmpty()) {
            System.out.println(">  Interface text:");
            System.out.println(segment.textInterface().indent(5));
        }
        for (var proc : segment.dictionary()) {
            if (proc == null) {
                System.out.println(">  Invalid procedure header.");
                continue;
            }
            switch (proc) {
                case PCodeProcedure pcode -> disassemble(pcode);
                case AssemblyProcedure asm -> disassemble(asm);
                default -> throw new RuntimeException("Unexpected procedure type: " + proc.getClass().getName());
            }
        }
    }

    public void disassemble(PCodeProcedure pcode) {
        System.out.printf(">  Proc#%d, Lex Lvl %d, Enter $%04x, Exit $%04x, Param %d, Data %d, JTAB=$%04x\n",
                pcode.procNum(), pcode.lexLevel(), pcode.enterIC(), pcode.exitIC(),
                pcode.paramsSize(), pcode.dataSize(), pcode.jumpTable());
        cpuSelection.instructionSet = InstructionSetPCode.forApplePascal();
        startAddress = pcode.enterIC();
        disassemble(pcode.codeBytes());
    }

    public void disassemble(AssemblyProcedure asm) {
        System.out.printf(">  ASM Proc, Relocation Segment #%d, Enter $%04x\n",
                asm.relocSegNum(), asm.enterIC());

        BiConsumer<int[], String> formatter = (table, name) -> {
            if (table.length > 0) {
                System.out.printf("\t%s-relative relocation table: ", name);
                for (int addr : table) System.out.printf("$%04X ", addr);
                System.out.println();
            }
        };
        formatter.accept(asm.baseRelativeReloc(), "base");
        formatter.accept(asm.segRelativeReloc(), "segment");
        formatter.accept(asm.procRelativeReloc(), "procedure");
        formatter.accept(asm.interpRelativeReloc(), "interpreter");

        var bb = ByteBuffer.wrap(asm.codeBytes());
        bb.order(ByteOrder.LITTLE_ENDIAN);
        for (int addr : asm.procRelativeReloc()) {
            int offset = addr - asm.enterIC();
            bb.putShort(offset, (short) (bb.getShort(offset) + asm.endIC()));
        }

        cpuSelection.instructionSet = InstructionSet6502.for6502();
        startAddress = asm.enterIC();
        disassemble(bb.array());
    }

    public void emitWithLabels(Instruction instruction) {
        int bytesPerLine = cpuSelection.instructionSet.defaults().bytesPerInstruction();
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
        System.out.printf("%-30s ", instruction.operands().stream().map(operand -> {
                if (operand.address().isPresent() && labels.containsKey(operand.address().get())) {
                    return operand.format(labels.get(operand.address().get()));
                }
                else {
                    return operand.format();
                }
            })
            .collect(Collectors.joining(",")));
        if (descriptions) {
            instruction.description().ifPresent(description -> {
                System.out.printf("; %s", description);
            });
        }
        System.out.println();

        if (code.length > bytesPerLine) {
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
        int bytesPerLine = cpuSelection.instructionSet.defaults().bytesPerInstruction();
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
        System.out.printf("%-30s", instruction.operands().stream().map(Instruction.Operand::format)
                .collect(Collectors.joining(",")));
        if (descriptions) {
            instruction.description().ifPresent(description -> {
                System.out.printf("; %s", description);
            });
        }
        System.out.println();

        if (code.length > bytesPerLine) {
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
        private Type type = Type.ASSEMBLY;
        
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
        @Option(names = { "--codefile", "--CODEFILE" }, description = "Apple Pascal CODEFILE")
        public void selectCODEFILE(boolean flag) {
            this.type = Type.CODEFILE;
            // A fake InstructionSet to prevent accidental NPE's.
            this.instructionSet = new InstructionSet() {
                @Override
                public Defaults defaults() {
                    return Defaults.builder()
                            .includeDescription(true)
                            .get();
                }

                @Override
                public List<Instruction> decode(Program program) {
                    return List.of();
                }

                @Override
                public List<OpcodeTable> opcodeTables() {
                    return List.of();
                }
            };
        }
        enum Type {
            ASSEMBLY, CODEFILE;
        }
    }

    static class TableBuilder {
        public static TableBuilder with(CommandLine.Help help) {
            return new TableBuilder(help);
        }

        private final CommandLine.Help help;
        private final List<String> headerText = new ArrayList<>();
        private final List<InstructionSet> columnObject = new ArrayList<>();
        private final List<String> rowLabels = new ArrayList<>();
        private final List<Function<InstructionSet,String>> rowFns = new ArrayList<>();

        private TableBuilder(CommandLine.Help help) {
            this.help = help;
        }
        public TableBuilder textHeader(String text) {
            assert headerText.isEmpty();
            headerText.add(text);
            return this;
        }
        public TableBuilder textHeader(String text, InstructionSet instructionSet) {
            assert !headerText.isEmpty();
            assert headerText.size() == columnObject.size()+1;
            headerText.add(text);
            columnObject.add(instructionSet);
            return this;
        }
        public TableBuilder row(String text, Function<InstructionSet,String> supplier) {
            rowLabels.add(text);
            rowFns.add(supplier);
            assert rowLabels.size() <= headerText.size();
            assert rowFns.size() < headerText.size();
            return this;
        }
        public TextTable build() {
            String[][] text = new String[rowLabels.size()][headerText.size()];
            int[] widths = new int[headerText.size()];
            for (int row=0; row<rowLabels.size(); row++) {
                text[row][0] = rowLabels.get(row);
                widths[0] = Math.max(widths[0], text[row][0].length());
                var rowFn = rowFns.get(row);
                for (int col=0; col<columnObject.size(); col++) {
                    text[row][col+1] = rowFn.apply(columnObject.get(col));
                    widths[col+1] = Math.max(widths[col+1], text[row][col+1].length());
                }
            }
            Column[] columns = new Column[headerText.size()];
            String[] seps = new String[headerText.size()];
            for (int i=0; i<headerText.size(); i++) {
                widths[i] = Math.max(widths[i], headerText.get(i).length());
                columns[i] = new Column(widths[i]+2, 2, Column.Overflow.WRAP);
                seps[i] = new String(new char[widths[i]]).replace('\0', '-');
            }
            TextTable table = TextTable.forColumns(help.colorScheme(), columns);
            table.addRowValues(headerText.toArray(new String[0]));
            table.addRowValues(seps);
            for (int row=0; row<rowLabels.size(); row++) {
                table.addRowValues(text[row]);
            }
            return table;
        }
    }
}
