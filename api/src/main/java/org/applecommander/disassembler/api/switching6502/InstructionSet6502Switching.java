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
package org.applecommander.disassembler.api.switching6502;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Queue;
import java.util.function.Function;

import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.Program;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;

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
    public int defaultStartAddress() {
        return 0x300;
    }

    @Override
    public Instruction decode(Program program) {
        if (!pending.isEmpty()) {
            return pending.remove();
        }
        return strategy.apply(program);
    }

    @Override
    public List<OpcodeTable> opcodeTables() {
        throw new RuntimeException("Not implemented");
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
        public byte[] getBytes() {
            return new byte[0];
        }
        
        @Override
        public Optional<String> getAddressLabel() {
            return Optional.empty();
        }
        
        @Override
        public void setAddressLabel(String label) {
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
        public void setOperandLabel(String label) {
        }

        @Override
        public String formatOperandWithValue() {
            return name;
        }

        @Override
        public String formatOperandWithLabel() {
            return formatOperandWithValue();
        }
    }
}
