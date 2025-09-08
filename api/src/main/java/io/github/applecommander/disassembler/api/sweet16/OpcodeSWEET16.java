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

public enum OpcodeSWEET16 {
    ADD, BC,  BK,  BM,  BM1, BNC, BNM1, BNZ, BP,   BR, 
    BS,  BZ,  CPR, DCR, INR, LD,  LDD,  POP, POPD, RS, 
    RTN, SET, ST,  STD, STP, SUB,
    // Unknown
    ZZZ("???");

    private String mnemonic;

    private OpcodeSWEET16() {
        this.mnemonic = name();
    }
    private OpcodeSWEET16(String mnemonic) {
        this.mnemonic = mnemonic;
    }
    
    public String getMnemonic() {
        return mnemonic;
    }
    
    public static final OpcodeSWEET16[] REGISTER_OPS = {
        //  0x  1x   2x   3x   4x    5x   6x   7x
        null,   SET, LD,  ST,  LD,   ST,  LDD, STD,
        //  8x  9x   Ax   Bx   Cx    Dx   Ex   Fx
        POP,    STP, ADD, SUB, POPD, CPR, INR, DCR 
    };
    public static final OpcodeSWEET16[] NON_REGISTER_OPS = {
        //0x 1x    2x   3x  4x  5x   6x   7x
        RTN, BR,   BNC, BC, BP, BM,  BZ,  BNZ,
        //8x 9x    Ax   Bx  Cx  Dx   Ex   Fx
        BM1, BNM1, BK,  RS, BS, ZZZ, ZZZ, ZZZ
    };
}
