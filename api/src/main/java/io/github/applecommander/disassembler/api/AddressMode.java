package io.github.applecommander.disassembler.api;

import java.util.function.BiFunction;

public enum AddressMode {
    ACC( 1, "%s"),
    ABS( 3, "%s $%04X"),
    ABSX(3, "%s $%04X,X"),
    ABSY(3, "%s $%04X,Y"),
    IMM( 2, "%s #$%02X"),
    IMP( 1, "%s"),
    IND( 2, "%s ($%04X)"),
    INDX(2, "%s ($%02X,X)"),
    INDY(2, "%s ($%02X),Y"),
    REL( 2, "%s $02X"),
    ZP(  2, "%s $%02X"),
    ZPX( 2, "%s $%02X,X"),
    ZPY( 2, "%s $%02X,Y");
    
    private int length;
    private String format;
    private BiFunction<String,Integer,String> formatFn;
    
    private AddressMode(int length, String format) {
        this.length = length;
        this.format = format;
        this.formatFn = (length == 0) ? this::format0 : this::format1;
    }
    
    public int getLength() {
        return length;
    }
    public String format(String mnemonic, int arg) {
        return formatFn.apply(mnemonic, arg);
    }
    
    private String format0(String mnemonic, int arg) {
        return String.format(format, mnemonic);
    }
    private String format1(String mnemonic, int arg) {
        return String.format(format, mnemonic, arg);
    }
    
    public static AddressMode[] MOS6502 = {
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

    public static AddressMode[] WDC65C02 = {
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