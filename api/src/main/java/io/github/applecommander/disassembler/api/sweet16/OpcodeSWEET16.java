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
    
    public static OpcodeSWEET16[] SWEET16 = {
        // See: https://en.wikipedia.org/wiki/SWEET16
        /*         -0    -1    -2    -3    -4    -5    -6    -7    -8    -9    -A    -B    -C    -D    -E    -F */
        /* 0- */  RTN,  BR,   BNC,  BC,   BP,   BM,   BZ,   BNZ,  BM1,  BNM1, BK,   RS,   BS,   ZZZ,  ZZZ,  ZZZ,
        /* 1- */  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET,  SET, 
        /* 2- */  LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   
        /* 3- */  ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,  
        /* 4- */  LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,   LD,
        /* 5- */  ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,   ST,
        /* 6- */  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  LDD,  
        /* 7- */  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  STD,  
        /* 8- */  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  POP,  
        /* 9- */  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  STP,  
        /* A- */  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  ADD,  
        /* B- */  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,  SUB,   
        /* C- */  POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, POPD, 
        /* D- */  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  CPR,  
        /* E- */  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  INR,  
        /* F- */  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  DCR,  
    };
}
