package io.github.applecommander.disassembler.api.switching6502;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Function;

import io.github.applecommander.disassembler.api.Instruction;
import io.github.applecommander.disassembler.api.InstructionSet;
import io.github.applecommander.disassembler.api.Program;
import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;
import io.github.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;

public class InstructionSet6502Switching implements InstructionSet {
    public static InstructionSet withSwitching() {
        return new InstructionSet6502Switching(InstructionSet6502.for6502(), InstructionSetSWEET16.forSWEET16());
    }
    
    private InstructionSet6502 mos6502;
    private InstructionSetSWEET16 sweet16;
    private Function<Program,Instruction> strategy = this::decode6502;
    private Queue<Instruction> pending = new LinkedList<>();
    
    private InstructionSet6502Switching(InstructionSet6502 mos6502, InstructionSetSWEET16 sweet16) {
        this.mos6502 = mos6502;
        this.sweet16 = sweet16;
    }

    @Override
    public Instruction decode(Program program) {
        if (!pending.isEmpty()) {
            return pending.remove();
        }
        return strategy.apply(program);
    }

    Instruction decode6502(Program program) {
        Instruction instruction = mos6502.decode(program);
        if ("JSR".equals(instruction.getOpcodeMnemonic()) && instruction.getOperandValue() == 0xf689) {
            strategy = this::decodeSWEET16;
            pending.add(Directive.of(".SWEET16", program.currentAddress()));
        }
        return instruction;
    }
    
    Instruction decodeSWEET16(Program program) {
        Instruction instruction = sweet16.decode(program);
        if ("RTN".equals(instruction.getOpcodeMnemonic())) {
            strategy = this::decode6502;
            pending.add(Directive.of(".6502", program.currentAddress()));
        }
        return instruction;
    }
    
    public static class Directive implements Instruction {
        public static Directive of(String name, int address) {
            return new Directive(name, address);
        }
        
        private String name;
        private int address;

        private Directive(String name, int address) {
            this.name = name;
            this.address = address;
        }
        
        @Override
        public int getAddress() {
            return address;
        }

        @Override
        public int getLength() {
            return 0;
        }

        @Override
        public byte[] getBytes() {
            return new byte[0];
        }

        @Override
        public String getOpcodeMnemonic() {
            return name;
        }

        @Override
        public boolean operandHasAddress() {
            return false;
        }

        @Override
        public int getOperandValue() {
            return 0;
        }

        @Override
        public String formatOperandWithValue() {
            return name;
        }

        @Override
        public String formatOperandWithLabel(String label) {
            return formatOperandWithValue();
        }
    }
}
