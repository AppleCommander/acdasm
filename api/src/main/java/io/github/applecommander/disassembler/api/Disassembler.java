package io.github.applecommander.disassembler.api;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;

public class Disassembler {
    private static Ini ini = new Ini();
    static {
        try (InputStream is = Disassembler.class.getResourceAsStream("/addresses.ini")) {
            ini.load(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int startAddress;
    private int bytesToSkip;
    private byte[] code;
    private InstructionSet instructionSet;
    private Map<Integer,String> labels = new HashMap<>();

    public static Builder with(byte[] code) {
        return new Builder(code);
    }
    
    public List<Instruction> decode() {
        List<Instruction> instructions = new ArrayList<>();
        Program program = new Program(code,startAddress);

        while (program.hasMore()) {
            Instruction instruction = null;
            if (program.currentOffset() < bytesToSkip) {
                instruction = InvalidInstruction.from(program);
            }
            else {
                instruction = instructionSet.decode(program);
            }
            instructions.add(instruction);
            
            boolean between = (instruction.getOperandValue() >= startAddress)
                           && (instruction.getOperandValue() < startAddress + code.length);
            if (between && instruction.operandHasAddress()) {
                labels.computeIfAbsent(instruction.getOperandValue(), addr -> String.format("L%04X", addr));
            }
        }

        for (Instruction instruction : instructions) {
            if (labels.containsKey(instruction.getAddress())) {
                instruction.setAddressLabel(labels.get(instruction.getAddress()));
            }
            if (instruction.operandHasAddress() && labels.containsKey(instruction.getOperandValue())) {
                instruction.setOperandLabel(labels.get(instruction.getOperandValue()));
            }
        }
        
        return instructions;
    }
    
    public static class Builder {
        private Set<String> sections = new HashSet<>();
        private Disassembler disassembler = new Disassembler();
        
        public Builder(byte[] code) {
            disassembler.startAddress = 0x300;
            disassembler.code = code;
            disassembler.instructionSet = InstructionSet6502.for6502();
        }
        public List<Instruction> decode() {
            // merge in all selected sections
            for (String name : sections) {
                Section section = ini.get(name);
                if (section == null) {
                    throw new RuntimeException(String.format("Section '%s' not defined.", name));
                }
                for (Map.Entry<String,String> entry : section.entrySet()) {
                    Optional<Integer> address = convert(entry.getValue());
                    if (address.isPresent()) {
                        disassembler.labels.putIfAbsent(address.get(), entry.getKey());
                    }
                }
            }
            
            return disassembler.decode();
        }
        
        public Builder startingAddress(int address) {
            disassembler.startAddress = address;
            return this;
        }
        public Builder bytesToSkip(int skip) {
            disassembler.bytesToSkip = skip;
            return this;
        }
        public Builder use(InstructionSet instructionSet) {
            disassembler.instructionSet = instructionSet;
            return this;
        }
        public Builder use6502() {
            disassembler.instructionSet = InstructionSet6502.for6502();
            return this;
        }
        public Builder use6502WithIllegalOpcodes() {
            disassembler.instructionSet = InstructionSet6502.for6502withIllegalInstructions();
            return this;
        }
        public Builder use65C02() {
            disassembler.instructionSet = InstructionSet6502.for65C02();
            return this;
        }
        
        public Builder section(List<String> names) {
            if (names != null) {
                names.forEach(this.sections::add);
            }
            return this;
        }
    }

    /** Add support for "$801" and "0x801" instead of just decimal like 2049. */
    public static Optional<Integer> convert(String value) {
        if (value == null) {
            return Optional.empty();
        } else if (value.startsWith("$")) {
            return Optional.of(Integer.valueOf(value.substring(1), 16));
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            return Optional.of(Integer.valueOf(value.substring(2), 16));
        } else {
            return Optional.of(Integer.valueOf(value));
        }
    }
    
    public static Set<String> sections() {
        return ini.keySet();
    }
}
