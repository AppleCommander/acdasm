package io.github.applecommander.disassembler.api;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;

public class Disassembler {
    private int startAddress;
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
            Instruction instruction = instructionSet.decode(program);
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
        private Disassembler disassembler = new Disassembler();
        
        public Builder(byte[] code) {
            disassembler.startAddress = 0x300;
            disassembler.code = code;
            disassembler.instructionSet = InstructionSet6502.for6502();
        }
        public List<Instruction> decode() {
            return disassembler.decode();
        }
        
        public Builder startingAddress(int address) {
            disassembler.startAddress = address;
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
    }
}
