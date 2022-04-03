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
    @Option(names = { "-a", "--addr", "--origin" }, defaultValue = "$300", converter = IntegerTypeConverter.class,
            description = "Set start address for application.")
    private int startAddress;
    
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
        int exitCode = new CommandLine(new Main()).execute(args);
        System.exit(exitCode);
    }
    
    @Override
    public Integer call() throws Exception {
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
    }
}
