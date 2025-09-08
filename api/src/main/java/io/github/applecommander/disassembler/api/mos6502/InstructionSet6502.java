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
package io.github.applecommander.disassembler.api.mos6502;

import io.github.applecommander.disassembler.api.Instruction;
import io.github.applecommander.disassembler.api.InstructionSet;
import io.github.applecommander.disassembler.api.Program;

public class InstructionSet6502 implements InstructionSet {
    public static InstructionSet6502 for6502() {
        return new InstructionSet6502(AddressMode6502.MOS6502, Opcode6502.MOS6502);
    }
    public static InstructionSet6502 for6502withIllegalInstructions() {
        return new InstructionSet6502(AddressMode6502.MOS6502, Opcode6502.MOS6502_WITH_ILLEGAL);
    }
    public static InstructionSet6502 for65C02() {
        return new InstructionSet6502(AddressMode6502.WDC65C02, Opcode6502.WDC65C02);
    }
    
    private AddressMode6502[] addressModes;
    private Opcode6502[] opcodes;
    
    private InstructionSet6502(AddressMode6502[] addressModes, Opcode6502[] opcodes) {
        this.addressModes = addressModes;
        this.opcodes = opcodes;
    }

    @Override
    public Instruction decode(Program program) {
        int op = Byte.toUnsignedInt(program.peek());
        
        AddressMode6502 addressMode = addressModes[op];
        Opcode6502 opcode = opcodes[op];

        if (isInvalidInstruction(opcode, op)) {
            addressMode = AddressMode6502.IMP;
        }
        
        int currentAddress = program.currentAddress();  // Need capture before read
        byte[] code = program.read(addressMode.getInstructionLength());
        
        return new Instruction6502(addressMode, opcode, currentAddress, code);
    }

    public boolean isInvalidInstruction(Opcode6502 opcode, int op) {
        if (opcode == Opcode6502.ZZZ) {
            return true;
        }
        else if (opcode == Opcode6502.NOP && op != 0xea) {
            return true;
        }
        return false;
    }
}
