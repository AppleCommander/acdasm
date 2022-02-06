package io.github.applecommander.disassembler.api.sweet16;

public enum AddressModeSWEET16 {
    CON(3, "%s R%x,$%04X"), // Constant (16-bit value)
    DIR(1, "%s R%x"),       // Direct (Rn)
    IND(1, "%s @R%x"),      // Indirect (@Rn)
    BRA(2, "%s $%04X"),     // Branch (2 + -128..+127)
    IMP(1, "%s");           // -

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

    public static AddressModeSWEET16[] SWEET16 = {
        // See: https://en.wikipedia.org/wiki/SWEET16
        /*         -0   -1   -2   -3   -4   -5   -6   -7   -8   -9   -A   -B   -C   -D   -E   -F */
        /* 0- */  IMP, BRA, BRA, BRA, BRA, BRA, BRA, BRA, BRA, BRA, IMP, IMP, BRA, IMP, IMP, IMP,
        /* 1- */  CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, CON, 
        /* 2- */  DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, 
        /* 3- */  DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, 
        /* 4- */  IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, 
        /* 5- */  IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND,  
        /* 6- */  IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND,
        /* 7- */  IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND,
        /* 8- */  IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND,
        /* 9- */  IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND,
        /* A- */  DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, 
        /* B- */  DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, 
        /* C- */  IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND, IND,
        /* D- */  DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, 
        /* E- */  DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, 
        /* F- */  DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, DIR, 
    };
}
