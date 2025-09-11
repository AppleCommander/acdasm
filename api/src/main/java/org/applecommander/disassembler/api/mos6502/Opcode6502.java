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

public enum Opcode6502 {
    // MOS 6502
    ADC, AND, ASL, BCC, BCS, BEQ,
    BIT, BMI, BNE, BPL, BRK, BVC,
    BVS, CLC, CLD, CLI, CLV, CMP,
    CPX, CPY, DEC, DEX, DEY, EOR,
    INC, INX, INY, JMP, JSR, LDA,
    LDX, LDY, LSR, NOP, ORA, PHA,
    PHP, PLA, PLP, ROL, ROR, RTI,
    RTS, SBC, SEC, SED, SEI, STA,
    STX, STY, TAX, TAY, TSX, TXA,
    TXS, TYA,
    // WDC 65C02
    BRA, PHX, PHY, PLX, PLY, STZ,
    TRB, TSB,
    // illegal opcodes
    ALR, ANC, ANE, ARR, DCP, ISC,
    LAS, LAX, LXA, RLA, RRA, SAX,
    SBX, SHA, SHX, SHY, SLO, SRE,
    TAS, USBC, JAM,
    // invalid
    ZZZ("???");
    
    private String mnemonic;

    private Opcode6502() {
        this.mnemonic = name();
    }
    private Opcode6502(String mnemonic) {
        this.mnemonic = mnemonic;
    }
    public String getMnemonic() {
        return mnemonic;
    }

    public static Opcode6502[] MOS6502 = {
            // See: https://www.masswerk.at/6502/6502_instruction_set.html
            /*         -0   -1   -2   -3   -4   -5   -6   -7   -8   -9  -A    -B   -C   -D   -E   -F */
            /* 0- */  BRK, ORA, ZZZ, ZZZ, ZZZ, ORA, ASL, ZZZ, PHP, ORA, ASL, ZZZ, ZZZ, ORA, ASL, ZZZ,
            /* 1- */  BPL, ORA, ZZZ, ZZZ, ZZZ, ORA, ASL, ZZZ, CLC, ORA, ZZZ, ZZZ, ZZZ, ORA, ASL, ZZZ,
            /* 2- */  JSR, AND, ZZZ, ZZZ, BIT, AND, ROL, ZZZ, PLP, AND, ROL, ZZZ, BIT, AND, ROL, ZZZ,  
            /* 3- */  BMI, AND, ZZZ, ZZZ, ZZZ, AND, ROL, ZZZ, SEC, AND, ZZZ, ZZZ, ZZZ, AND, ROL, ZZZ,
            /* 4- */  RTI, EOR, ZZZ, ZZZ, ZZZ, EOR, LSR, ZZZ, PHA, EOR, LSR, ZZZ, JMP, EOR, LSR, ZZZ,
            /* 5- */  BVC, EOR, ZZZ, ZZZ, ZZZ, EOR, LSR, ZZZ, CLI, EOR, ZZZ, ZZZ, ZZZ, EOR, LSR, ZZZ,
            /* 6- */  RTS, ADC, ZZZ, ZZZ, ZZZ, ADC, ROR, ZZZ, PLA, ADC, ROR, ZZZ, JMP, ADC, ROR, ZZZ,
            /* 7- */  BVS, ADC, ZZZ, ZZZ, ZZZ, ADC, ROR, ZZZ, SEI, ADC, ZZZ, ZZZ, ZZZ, ADC, ROR, ZZZ,
            /* 8- */  ZZZ, STA, ZZZ, ZZZ, STY, STA, STX, ZZZ, DEY, ZZZ, TXA, ZZZ, STY, STA, STX, ZZZ,
            /* 9- */  BCC, STA, ZZZ, ZZZ, STY, STA, STX, ZZZ, TYA, STA, TXS, ZZZ, ZZZ, STA, ZZZ, ZZZ,
            /* A- */  LDY, LDA, LDX, ZZZ, LDY, LDA, LDX, ZZZ, TAY, LDA, TAX, ZZZ, LDY, LDA, LDX, ZZZ,
            /* B- */  BCS, LDA, ZZZ, ZZZ, LDY, LDA, LDX, ZZZ, CLV, LDA, TSX, ZZZ, LDY, LDA, LDX, ZZZ,
            /* C- */  CPY, CMP, ZZZ, ZZZ, CPY, CMP, DEC, ZZZ, INY, CMP, DEX, ZZZ, CPY, CMP, DEC, ZZZ,
            /* D- */  BNE, CMP, ZZZ, ZZZ, ZZZ, CMP, DEC, ZZZ, CLD, CMP, ZZZ, ZZZ, ZZZ, CMP, DEC, ZZZ,
            /* E- */  CPX, SBC, ZZZ, ZZZ, CPX, SBC, INC, ZZZ, INX, SBC, NOP, ZZZ, CPX, SBC, INC, ZZZ,
            /* F- */  BEQ, SBC, ZZZ, ZZZ, ZZZ, SBC, INC, ZZZ, SED, SBC, ZZZ, ZZZ, ZZZ, SBC, INC, ZZZ
        };
    public static Opcode6502[] MOS6502_WITH_ILLEGAL = {
            // See: https://www.masswerk.at/6502/6502_instruction_set.html (and show illegal opcodes)
            /*         -0   -1   -2   -3   -4   -5   -6   -7   -8   -9  -A     -B   -C   -D   -E   -F */
            /* 0- */  BRK, ORA, JAM, SLO, NOP, ORA, ASL, SLO, PHP, ORA, ASL,  ANC, NOP, ORA, ASL, SLO,
            /* 1- */  BPL, ORA, JAM, SLO, NOP, ORA, ASL, SLO, CLC, ORA, NOP,  SLO, NOP, ORA, ASL, SLO,
            /* 2- */  JSR, AND, JAM, RLA, BIT, AND, ROL, RLA, PLP, AND, ROL,  ANC, BIT, AND, ROL, RLA,  
            /* 3- */  BMI, AND, JAM, RLA, NOP, AND, ROL, RLA, SEC, AND, NOP,  RLA, NOP, AND, ROL, RLA,
            /* 4- */  RTI, EOR, JAM, SRE, NOP, EOR, LSR, SRE, PHA, EOR, LSR,  ALR, JMP, EOR, LSR, SRE,
            /* 5- */  BVC, EOR, JAM, SRE, NOP, EOR, LSR, SRE, CLI, EOR, NOP,  SRE, NOP, EOR, LSR, SRE,
            /* 6- */  RTS, ADC, JAM, RRA, NOP, ADC, ROR, RRA, PLA, ADC, ROR,  ARR, JMP, ADC, ROR, RRA,
            /* 7- */  BVS, ADC, JAM, RRA, NOP, ADC, ROR, RRA, SEI, ADC, NOP,  RRA, NOP, ADC, ROR, RRA,
            /* 8- */  NOP, STA, NOP, SAX, STY, STA, STX, SAX, DEY, NOP, TXA,  ANE, STY, STA, STX, SAX,
            /* 9- */  BCC, STA, JAM, SHA, STY, STA, STX, SAX, TYA, STA, TXS,  TAS, SHY, STA, SHX, SHA,
            /* A- */  LDY, LDA, LDX, LAX, LDY, LDA, LDX, LAX, TAY, LDA, TAX,  LXA, LDY, LDA, LDX, LAX,
            /* B- */  BCS, LDA, JAM, LAX, LDY, LDA, LDX, LAX, CLV, LDA, TSX,  LAS, LDY, LDA, LDX, LAX,
            /* C- */  CPY, CMP, NOP, DCP, CPY, CMP, DEC, DCP, INY, CMP, DEX,  SBX, CPY, CMP, DEC, DCP,
            /* D- */  BNE, CMP, JAM, DCP, NOP, CMP, DEC, DCP, CLD, CMP, NOP,  DCP, NOP, CMP, DEC, DCP,
            /* E- */  CPX, SBC, NOP, ISC, CPX, SBC, INC, ISC, INX, SBC, NOP, USBC, CPX, SBC, INC, ISC,
            /* F- */  BEQ, SBC, JAM, ISC, NOP, SBC, INC, ISC, SED, SBC, NOP,  ISC, NOP, SBC, INC, ISC
        };
    public static Opcode6502[] WDC65C02 = {
            // See: http://6502.org/tutorials/65c02opcodes.html
            /*         -0   -1   -2   -3   -4   -5   -6   -7   -8   -9  -A    -B   -C   -D   -E   -F */
            /* 0- */  BRK, ORA, ZZZ, ZZZ, TSB, ORA, ASL, ZZZ, PHP, ORA, ASL, ZZZ, TSB, ORA, ASL, ZZZ,
            /* 1- */  BPL, ORA, ORA, ZZZ, TRB, ORA, ASL, ZZZ, CLC, ORA, INC, ZZZ, TRB, ORA, ASL, ZZZ,
            /* 2- */  JSR, AND, ZZZ, ZZZ, BIT, AND, ROL, ZZZ, PLP, AND, ROL, ZZZ, BIT, AND, ROL, ZZZ,  
            /* 3- */  BMI, AND, AND, ZZZ, BIT, AND, ROL, ZZZ, SEC, AND, DEC, ZZZ, BIT, AND, ROL, ZZZ,
            /* 4- */  RTI, EOR, ZZZ, ZZZ, ZZZ, EOR, LSR, ZZZ, PHA, EOR, LSR, ZZZ, JMP, EOR, LSR, ZZZ,
            /* 5- */  BVC, EOR, EOR, ZZZ, ZZZ, EOR, LSR, ZZZ, CLI, EOR, PHY, ZZZ, ZZZ, EOR, LSR, ZZZ,
            /* 6- */  RTS, ADC, ZZZ, STZ, ZZZ, ADC, ROR, ZZZ, PLA, ADC, ROR, ZZZ, JMP, ADC, ROR, ZZZ,
            /* 7- */  BVS, ADC, ADC, STZ, ZZZ, ADC, ROR, ZZZ, SEI, ADC, PLY, ZZZ, JMP, ADC, ROR, ZZZ,
            /* 8- */  BRA, STA, ZZZ, ZZZ, STY, STA, STX, ZZZ, DEY, BIT, TXA, ZZZ, STY, STA, STX, ZZZ,
            /* 9- */  BCC, STA, STA, ZZZ, STY, STA, STX, ZZZ, TYA, STA, TXS, ZZZ, STZ, STA, STZ, ZZZ,
            /* A- */  LDY, LDA, LDX, ZZZ, LDY, LDA, LDX, ZZZ, TAY, LDA, TAX, ZZZ, LDY, LDA, LDX, ZZZ,
            /* B- */  BCS, LDA, LDA, ZZZ, LDY, LDA, LDX, ZZZ, CLV, LDA, TSX, ZZZ, LDY, LDA, LDX, ZZZ,
            /* C- */  CPY, CMP, ZZZ, ZZZ, CPY, CMP, DEC, ZZZ, INY, CMP, DEX, ZZZ, CPY, CMP, DEC, ZZZ,
            /* D- */  BNE, CMP, CMP, ZZZ, ZZZ, CMP, DEC, ZZZ, CLD, CMP, PHX, ZZZ, ZZZ, CMP, DEC, ZZZ,
            /* E- */  CPX, SBC, ZZZ, ZZZ, CPX, SBC, INC, ZZZ, INX, SBC, NOP, ZZZ, CPX, SBC, INC, ZZZ,
            /* F- */  BEQ, SBC, SBC, ZZZ, ZZZ, SBC, INC, ZZZ, SED, SBC, PLX, ZZZ, ZZZ, SBC, INC, ZZZ
        };

}