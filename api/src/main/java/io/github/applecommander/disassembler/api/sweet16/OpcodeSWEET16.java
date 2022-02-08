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
