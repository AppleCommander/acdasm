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

public enum OpcodeSWEET16 {
    ADD, BC,  BK,  BM,  BM1, BNC, BNM1, BNZ, BP,   BR, 
    BS,  BZ,  CPR, DCR, INR, LD,  LDD,  POP, POPD, RS, 
    RTN, SET, ST,  STD, STP, SUB,
    // Unknown
    ZZZ("???");

    private final String mnemonic;

    OpcodeSWEET16() {
        this.mnemonic = name();
    }
    OpcodeSWEET16(String mnemonic) {
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
