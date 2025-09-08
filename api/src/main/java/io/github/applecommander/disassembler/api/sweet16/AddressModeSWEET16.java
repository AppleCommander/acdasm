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
package io.github.applecommander.disassembler.api.sweet16;

public enum AddressModeSWEET16 {
    CON(3, "%s R%X,#%s"), // Constant (16-bit value)
    ABS(3, "%s R%X,%s"),  // Absolute address
    DIR(1, "%s R%X"),      // Direct (Rn)
    IND(1, "%s @R%X"),     // Indirect (@Rn)
    BRA(2, "%s %s"),      // Branch (2 + -128..+127)
    IMP(1, "%s");          // -

    private int instructionLength;
    private String instructionFormat;
    
    private AddressModeSWEET16(int instructionLength, String instructionFormat) {
        this.instructionLength = instructionLength;
        this.instructionFormat = instructionFormat;
    }

    public int getInstructionLength() {
        return instructionLength;
    }
    public String getInstructionFormat() {
        return instructionFormat;
    }
    public boolean isOperandAbsoluteAddress() {
        return is(ABS);
    }
    public boolean isOperandRelativeAddress() {
        return is(BRA);
    }
    public boolean doesOperandRequireRegister() {
        return is(CON, ABS, DIR, IND);
    }
    private boolean is(final AddressModeSWEET16... addressModes) {
        for (AddressModeSWEET16 am : addressModes) {
            if (this == am) return true;
        }
        return false;
    }
    
    public static AddressModeSWEET16[] REGISTER_OPS = {
        //0x  1x   2x   3x   4x   5x   6x   7x
        null, CON, DIR, DIR, IND, IND, IND, IND, 
        //8x  9x   Ax   Bx   Cx   Dx   Ex   Fx
        IND,  IND, DIR, DIR, IND, DIR, DIR, DIR
    };
    public static AddressModeSWEET16[] NON_REGISTER_OPS = {
        //0x 1x   2x   3x   4x   5x   6x   7x
        IMP, BRA, BRA, BRA, BRA, BRA, BRA, BRA, 
        //8x 9x   Ax   Bx   Cx   Dx   Ex   Fx
        BRA, BRA, IMP, IMP, BRA, IMP, IMP, IMP,
    };
}
