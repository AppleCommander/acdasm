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
    // FIXME the indirect and ZP are mixed a bit (zp) vs (addr) vs (zp,x) vs (addr,x).
    ACC( 1, "%s"),
    ABS( 3, "%s %s"),
    ABSX(3, "%s %s,X"),
    ABSY(3, "%s %s,Y"),
    IMM( 2, "%s #%s"),
    IMP( 1, "%s"),
    IND( 2, "%s (%s)"),
    INDX(2, "%s (%s,X)"),
    INDY(2, "%s (%s),Y"),
    REL( 2, "%s %s"),
    ZP(  2, "%s %s"),
    ZPX( 2, "%s %s,X"),
    ZPY( 2, "%s %s,Y");
    
    private int instructionLength;
    private String instructionFormat;
    
    private AddressMode6502(int instructionLength, String instructionFormat) {
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
        return in(ABS, ABSX, ABSY);
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
    
    public static AddressMode6502[] MOS6502 = {
        // See: https://www.masswerk.at/6502/6502_instruction_set.html (and show illegal opcodes)
        /*         -0    -1   -2   -3   -4   -5   -6   -7   -8    -9   -A    -B    -C    -D    -E    -F */
        /* 0- */  IMP, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* 1- */  REL, INDY, IMM, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* 2- */  ABS, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* 3- */  REL, INDY, IMM, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* 4- */  IMP, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* 5- */  REL, INDY, IMM, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* 6- */  IMP, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,  IND,  ABS,  ABS,  ABS,
        /* 7- */  REL, INDY, IMM, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* 8- */  IMM, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* 9- */  REL, INDY, IMM, ZPY, ZPX, ZPX, ZPY, ZPY, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSY, ABSY,
        /* A- */  IMM, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* B- */  REL, INDY, IMM, ZPY, ZPX, ZPX, ZPY, ZPY, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSY, ABSY,
        /* C- */  IMM, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* D- */  REL, INDY, IMM, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* E- */  IMM, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* F- */  REL, INDY, IMM, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
    };

    public static AddressMode6502[] WDC65C02 = {
        // See: http://6502.org/tutorials/65c02opcodes.html
        /*         -0    -1   -2   -3   -4   -5   -6   -7   -8    -9   -A    -B    -C    -D    -E    -F */
        /* 0- */  IMP, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* 1- */  REL, INDY,  ZP, ZPY,  ZP, ZPX, ZPX, ZPX, IMP, ABSY, ACC, ABSY,  ABS, ABSX, ABSX, ABSX,
        /* 2- */  ABS, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* 3- */  REL, INDY,  ZP, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, ACC, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* 4- */  IMP, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* 5- */  REL, INDY,  ZP, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* 6- */  IMP, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, ACC,  IMM,  IND,  ABS,  ABS,  ABS,
        /* 7- */  REL, INDY,  ZP, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* 8- */  REL, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* 9- */  REL, INDY,  ZP, ZPY, ZPX, ZPX, ZPY, ZPY, IMP, ABSY, IMP, ABSY,  ABS, ABSX, ABSX, ABSY,
        /* A- */  IMM, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* B- */  REL, INDY,  ZP, ZPY, ZPX, ZPX, ZPY, ZPY, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSY, ABSY,
        /* C- */  IMM, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* D- */  REL, INDY,  ZP, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
        /* E- */  IMM, INDX, IMM, ZPX,  ZP,  ZP,  ZP,  ZP, IMP,  IMM, IMP,  IMM,  ABS,  ABS,  ABS,  ABS,
        /* F- */  REL, INDY,  ZP, ZPY, ZPX, ZPX, ZPX, ZPX, IMP, ABSY, IMP, ABSY, ABSX, ABSX, ABSX, ABSX,
    };
}