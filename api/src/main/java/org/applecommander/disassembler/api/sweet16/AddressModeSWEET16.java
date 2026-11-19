/*
 * Copyright 2026  Robert Greene
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
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
