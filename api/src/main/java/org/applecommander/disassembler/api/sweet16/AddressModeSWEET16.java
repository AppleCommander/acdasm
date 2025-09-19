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
package org.applecommander.disassembler.api.sweet16;

public enum AddressModeSWEET16 {
    CON(3),     // Constant (16-bit value)
    DIR(1),     // Direct (Rn)
    IND(1),     // Indirect (@Rn)
    BRA(2),     // Branch (2 + -128..+127)
    IMP(1);     // Other

    private final int instructionLength;

    AddressModeSWEET16(int instructionLength) {
        this.instructionLength = instructionLength;
    }

    public int getInstructionLength() {
        return instructionLength;
    }
    public boolean isOperandRelativeAddress() {
        return is(BRA);
    }
    public boolean doesOperandRequireRegister() {
        return is(CON, DIR, IND);
    }
    private boolean is(final AddressModeSWEET16... addressModes) {
        for (AddressModeSWEET16 am : addressModes) {
            if (this == am) return true;
        }
        return false;
    }
    
    public static final AddressModeSWEET16[] REGISTER_OPS = {
        //0x  1x   2x   3x   4x   5x   6x   7x
        null, CON, DIR, DIR, IND, IND, IND, IND, 
        //8x  9x   Ax   Bx   Cx   Dx   Ex   Fx
        IND,  IND, DIR, DIR, IND, DIR, DIR, DIR
    };
    public static final AddressModeSWEET16[] NON_REGISTER_OPS = {
        //0x 1x   2x   3x   4x   5x   6x   7x
        IMP, BRA, BRA, BRA, BRA, BRA, BRA, BRA, 
        //8x 9x   Ax   Bx   Cx   Dx   Ex   Fx
        BRA, BRA, IMP, IMP, BRA, IMP, IMP, IMP,
    };
}
