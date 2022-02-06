package io.github.applecommander.disassembler.api;

import java.util.ArrayList;
import java.util.List;

import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;

public class Disassembler {
    private int startAddress;
    private byte[] code;
    private InstructionSet instructionSet;

    public static Builder with(byte[] code) {
        return new Builder(code);
    }
    
    public List<Instruction> decode() {
        List<Instruction> instructions = new ArrayList<>();
        Program program = new Program(code,startAddress);

        while (program.hasMore()) {
            Instruction instruction = instructionSet.decode(program);
            instructions.add(instruction);
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
