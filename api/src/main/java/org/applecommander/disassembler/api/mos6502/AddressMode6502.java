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
package org.applecommander.disassembler.api.mos6502;

public enum AddressMode6502 {
    ACC(     1),
    ABS(     3),
    ABSX(    3),
    ABSY(    3),
    IMM(     2),
    IMP(     1),
    INDABS(  3),
    INDABSX( 3),
    INDZP(   2),
    INDZPX(  2),
    INDZPY(  2),
    REL(     2),
    ZP(      2),
    ZPX(     2),
    ZPY(     2),
    // For 65C02 extra NOPs:
    ZZZ1(    1),
    ZZZ2(    2),
    ZZZ3(    3);
    
    private final int instructionLength;

    AddressMode6502(int instructionLength) {
        this.instructionLength = instructionLength;
    }
    
    public int getInstructionLength() {
        return instructionLength;
    }
    public boolean isOperandAbsoluteAddress() {
        return in(ABS, ABSX, ABSY, INDABS, INDABSX);
    }
    public boolean isOperandRelativeAddress() {
        return in(REL);
    }
    
    private boolean in(final AddressMode6502... addressModes) {
        for (AddressMode6502 am : addressModes) {
            if (this == am) return true;
        }
        return false;
    }
    
    public static final AddressMode6502[] MOS6502 = {
        // See: https://www.masswerk.at/6502/6502_instruction_set.html (and show illegal opcodes)
        /*         -0      -1   -2      -3   -4   -5   -6   -7   -8    -9   -A    -B      -C    -D    -E    -F */
        /* 0- */  IMP, INDZPX, IMP, INDZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,    ABS,  ABS,  ABS,  ABS,
        /* 1- */  REL, INDZPY, IMP, INDZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY,   ABSX, ABSX, ABSX, ABSX,
        /* 2- */  ABS, INDZPX, IMP, INDZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,    ABS,  ABS,  ABS,  ABS,
        /* 3- */  REL, INDZPY, IMP, INDZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY,   ABSX, ABSX, ABSX, ABSX,
        /* 4- */  IMP, INDZPX, IMP, INDZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,    ABS,  ABS,  ABS,  ABS,
        /* 5- */  REL, INDZPY, IMP, INDZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY,   ABSX, ABSX, ABSX, ABSX,
        /* 6- */  IMP, INDZPX, IMP, INDZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM, INDABS,  ABS,  ABS,  ABS,
        /* 7- */  REL, INDZPY, IMP, INDZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY,   ABSX, ABSX, ABSX, ABSX,
        /* 8- */  IMM, INDZPX, IMM, INDZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,    ABS,  ABS,  ABS,  ABS,
        /* 9- */  REL, INDZPY, IMP, INDZPY, ZPX, ZPX, ZPY, ZPY, IMP, ABSY, IMP, ABSY,   ABSX, ABSX, ABSY, ABSY,
        /* A- */  IMM, INDZPX, IMM, INDZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,    ABS,  ABS,  ABS,  ABS,
        /* B- */  REL, INDZPY, IMP, INDZPY, ZPX, ZPX, ZPY, ZPY, IMP, ABSY, IMP, ABSY,   ABSX, ABSX, ABSY, ABSY,
        /* C- */  IMM, INDZPX, IMM, INDZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,    ABS,  ABS,  ABS,  ABS,
        /* D- */  REL, INDZPY, IMP, INDZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY,   ABSX, ABSX, ABSX, ABSX,
        /* E- */  IMM, INDZPX, IMM, INDZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,    ABS,  ABS,  ABS,  ABS,
        /* F- */  REL, INDZPY, IMP, INDZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY,   ABSX, ABSX, ABSX, ABSX,
    };

    public static final AddressMode6502[] WDC65C02 = {
        // See: http://6502.org/tutorials/65c02opcodes.html
        /*         -0      -1     -2    -3    -4   -5   -6  -7   -8    -9   -A    -B       -C    -D    -E   -F */
        /* 0- */  IMP, INDZPX,  ZZZ2, ZZZ1,   ZP,  ZP,  ZP, ZP, IMP,  IMM, ACC, ZZZ1,     ABS,  ABS,  ABS, REL,
        /* 1- */  REL, INDZPY, INDZP, ZZZ1,   ZP, ZPX, ZPX, ZP, IMP, ABSY, ACC, ZZZ1,     ABS, ABSX, ABSX, REL,
        /* 2- */  ABS, INDZPX,  ZZZ2, ZZZ1,   ZP,  ZP,  ZP, ZP, IMP,  IMM, ACC, ZZZ1,     ABS,  ABS,  ABS, REL,
        /* 3- */  REL, INDZPY, INDZP, ZZZ1,  ZPX, ZPX, ZPX, ZP, IMP, ABSY, ACC, ZZZ1,    ABSX, ABSX, ABSX, REL,
        /* 4- */  IMP, INDZPX,  ZZZ2, ZZZ1, ZZZ2,  ZP,  ZP, ZP, IMP,  IMM, ACC, ZZZ1,     ABS,  ABS,  ABS, REL,
        /* 5- */  REL, INDZPY, INDZP, ZZZ1, ZZZ2, ZPX, ZPX, ZP, IMP, ABSY, IMP, ZZZ1,    ZZZ3, ABSX, ABSX, REL,
        /* 6- */  IMP, INDZPX,  ZZZ2, ZZZ1,   ZP,  ZP,  ZP, ZP, IMP,  IMM, ACC, ZZZ1,  INDABS,  ABS,  ABS, REL,
        /* 7- */  REL, INDZPY, INDZP, ZZZ1,  ZPX, ZPX, ZPX, ZP, IMP, ABSY, IMP, ZZZ1, INDABSX, ABSX, ABSX, REL,
        /* 8- */  REL, INDZPX,  ZZZ2, ZZZ1,   ZP,  ZP,  ZP, ZP, IMP,  IMM, IMP, ZZZ1,     ABS,  ABS,  ABS, REL,
        /* 9- */  REL, INDZPY, INDZP, ZZZ1,  ZPX, ZPX, ZPY, ZP, IMP, ABSY, IMP, ZZZ1,     ABS, ABSX, ABSX, REL,
        /* A- */  IMM, INDZPX,   IMM, ZZZ1,   ZP,  ZP,  ZP, ZP, IMP,  IMM, IMP, ZZZ1,     ABS,  ABS,  ABS, REL,
        /* B- */  REL, INDZPY, INDZP, ZZZ1,  ZPX, ZPX, ZPY, ZP, IMP, ABSY, IMP, ZZZ1,    ABSX, ABSX, ABSY, REL,
        /* C- */  IMM, INDZPX,  ZZZ2, ZZZ1,   ZP,  ZP,  ZP, ZP, IMP,  IMM, IMP,  IMM,     ABS,  ABS,  ABS, REL,
        /* D- */  REL, INDZPY, INDZP, ZZZ1, ZZZ2, ZPX, ZPX, ZP, IMP, ABSY, IMP, ABSY,    ZZZ3, ABSX, ABSX, REL,
        /* E- */  IMM, INDZPX,  ZZZ2, ZZZ1,   ZP,  ZP,  ZP, ZP, IMP,  IMM, IMP, ZZZ1,     ABS,  ABS,  ABS, REL,
        /* F- */  REL, INDZPY, INDZP, ZZZ1, ZZZ2, ZPX, ZPX, ZP, IMP, ABSY, IMP, ZZZ1,    ZZZ3, ABSX, ABSX, REL,
    };
}