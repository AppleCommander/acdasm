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
package io.github.applecommander.disassembler.api.z80;

import io.github.applecommander.disassembler.api.Instruction;
import io.github.applecommander.disassembler.api.InstructionSet;
import io.github.applecommander.disassembler.api.Program;

import java.util.Set;

import static io.github.applecommander.disassembler.api.z80.InstructionSetZ80.Flag.*;

public class InstructionSetZ80 implements InstructionSet {
    public static InstructionSetZ80 forZ80() {
        return new InstructionSetZ80();
    }

    // Prevent construction
    private InstructionSetZ80() {}

    @Override
    public int defaultStartAddress() {
        return 0x100;   // This is the start address for COM files
    }

    @Override
    public Instruction decode(Program program) {
        int addr = program.currentAddress();

        int length = 1;
        int b = Byte.toUnsignedInt(program.peek());
        Opcode op = ROOT_OPCODES[b];
        boolean ix = false;
        boolean iy = false;
        // Overrides first
        if (op.flags.contains(OVERRIDE)) {
            ix = op.opcode == 0xdd;
            iy = op.opcode == 0xfd;
            b = Byte.toUnsignedInt(program.peek(length));
            op = ROOT_OPCODES[b];
            length++;
        }
        // Alternate prefixes next
        if (op.flags.contains(PREFIX)) {
            b = Byte.toUnsignedInt(program.peek(length));
            if (op.opcode() == 0xed) {
                op = ED_OPCODES[b];
            }
            else if (op.opcode() == 0xcb) {
                op = CB_OPCODES[b];
            }
            length++;
        }
        // The paramaters (add'l bytes)
        String operandFmt = op.fmt;
        int operandValue = 0;
        if ((op.flags.contains(DATLO) && op.flags.contains(DATHI))
                || (op.flags.contains(ADDLO) && op.flags.contains(ADDHI))) {
            int b1 = Byte.toUnsignedInt(program.peek(length));
            int b2 = Byte.toUnsignedInt(program.peek(length+1));
            operandValue = b1 | b2 << 8;
            String param = op.flags.contains(DATLO) ? "data" : "add";
            operandFmt = operandFmt.replace(param, String.format("%04XH", b1 | b2 << 8));
            length += 2;
        }
        if (op.flags.contains(DATA) || op.flags.contains(PORT)) {
            operandValue = Byte.toUnsignedInt(program.peek(length));
            String param = op.flags.contains(DATA) ? "data" : "port";
            operandFmt = operandFmt.replace(param, String.format("%02XH", operandValue));
            length += 1;
        }
        if (op.flags.contains(OFFSET)) {
            operandValue = addr + Byte.toUnsignedInt(program.peek(length));
            operandFmt = operandFmt.replace("offset", String.format("%04XH", operandValue));
            length += 1;
        }
        // Handle IX / IY
        if (ix || iy) {
            String reg = ix ? "IX" : "IY";
            if (operandFmt.contains("(HL)")) {
                int displacement = Byte.toUnsignedInt(program.peek(length));
                operandFmt = operandFmt.replace("(HL)", String.format("(%s+%02XH)", reg, displacement));
                length++;
            }
            else if (operandFmt.contains("HL")) {
                operandFmt = operandFmt.replace("HL", reg);
            }
        }
        //
        return new InstructionZ80(addr, op.mnemonic, operandFmt, operandValue, program.read(length));
    }

    @Override
    public String name() {
        return "Z80";
    }

    @Override
    public String opcodeExample(int op) {
        Opcode opcode = ROOT_OPCODES[op];
        String fmt = opcode.fmt
                .replace("rp","rr")         // register pair
                .replace("data", "VALUE")   // both 8-bit and 16-bit values
                .replace("ddd", "r")        // register
                .replace("sss", "r")        // register
                .replace("offset", "ADDR")  // address offset
                .replace("add", "ADDR");    // address
        // cc remains as condition code
        // n remains as n
        // port remains as port
        return String.format("%s %s", opcode.mnemonic, fmt);
    }

    record Opcode(int opcode, String mnemonic, String fmt, Set<Flag> flags) {
        public Opcode(int opcode, String mnemonic, String fmt, Flag... flags) {
            this(opcode, mnemonic, fmt, Set.of(flags));
        }
    }

    private static final Opcode[] ROOT_OPCODES = new Opcode[256];
    private static final Opcode[] ED_OPCODES = new Opcode[256];
    private static final Opcode[] CB_OPCODES = new Opcode[256];
    static {
        with(ROOT_OPCODES)
                .add(0b00000000, "NOP")
                .add(0b00000001, "LD", "rp,data", RP2, DATLO, DATHI)
                .add(0b00000010, "LD", "(rp),A", RP1)
                .add(0b00000011, "INC", "rp", RP2)
                .add(0b00000100, "INC", "ddd", DDD)
                .add(0b00000101, "DEC", "ddd", DDD)
                .add(0b00000110, "LD", "ddd,data", DDD, DATA)
                .add(0b00000111, "RLCA")
                .add(0b00001001, "ADD", "rp", RP2)
                .add(0b00001010, "LD", "A,(rp)", RP1)
                .add(0b00001011, "DEC", "rp", RP2)
                .add(0b00001000, "EX", "AF,AF'")
                .add(0b00001111, "RRCA")
                .add(0b00010000, "DJNZ", "offset", OFFSET)
                .add(0b00010111, "RLA")
                .add(0b00011000, "JR", "offset", OFFSET)
                .add(0b00011111, "RRA")
                .add(0b00100000, "JR", "cc,offset", CC2, OFFSET)
                .add(0b00100010, "LD", "add,HL", ADDLO, ADDHI)
                .add(0b00100111, "DAA")
                .add(0b00101010, "LD", "HL,add", ADDLO, ADDHI)
                .add(0b00101111, "CPL")
                .add(0b00110010, "LD", "add,A", ADDLO, ADDHI)
                .add(0b00110111, "SCF")
                .add(0b00111010, "LD", "A,add", ADDLO, ADDHI)
                .add(0b00111111, "CCF")
                .add(0b01000000, "LD", "ddd,sss", DDD, SSS)
                .add(0b01110110, "HALT")
                .add(0b10000000, "ADD,ADC,SUB,SBC,AND,XOR,OR,CP", "sss", SSS, ALU)
                .add(0b11000000, "RET", "cc", CC3)
                .add(0b11000001, "POP", "rp", RP2)
                .add(0b11000010, "JP", "cc,add", CC3, ADDLO, ADDHI)
                .add(0b11000011, "JP", "add", ADDLO, ADDHI)
                .add(0b11000100, "CALL", "cc,add", CC3, ADDLO, ADDHI)
                .add(0b11000101, "PUSH", "rp", RP2)
                .add(0b11000110, "ADD,ADC,SUB,SBC,AND,XOR,OR,CP", "data", ALU, DATA)
                .add(0b11000111, "RST", "n", N3)
                .add(0b11001001, "RET")
                .add(0b11001011, "CB", "", PREFIX)
                .add(0b11001101, "CALL", "add", ADDLO, ADDHI)
                .add(0b11010011, "OUT", "port,A", PORT)
                .add(0b11011001, "EXX")
                .add(0b11011011, "IN", "A,port", PORT)
                .add(0b11011101, "IX", "", OVERRIDE)
                .add(0b11100011, "EX", "(SP),HL")
                .add(0b11101001, "JP", "(HL)")
                .add(0b11101011, "EX", "DE,HL")
                .add(0b11101101, "ED", "", PREFIX)
                .add(0b11110011, "DI")
                .add(0b11111001, "LD", "SP,HL")
                .add(0b11111011, "EI")
                .add(0b11111101, "IY", "", OVERRIDE);
        with(ED_OPCODES)
                .add(0b01000000, "IN", "ddd,(C)", DDD)
                .add(0b01000001, "OUT", "(C),ddd", DDD)
                .add(0b01000010, "SBC", "HL,rp", RP2)
                .add(0b01000011, "LD", "(add),rp", ADDLO, ADDHI, RP2)
                .add(0b01000100, "NEG")
                .add(0b01000101, "RETN")
                .add(0b01000110, "IM", "n", N2)
                .add(0b01000111, "LD", "I,A")
                .add(0b01001010, "ADC", "HL,rp", RP2)
                .add(0b01001011, "LD", "rp,(add)", ADDLO, ADDHI, RP2)
                .add(0b01001101, "RETI")
                .add(0b01001111, "LD", "R,A")
                .add(0b01010111, "LD", "A,I")
                .add(0b01011111, "LD", "A,R")
                .add(0b01100111, "RRD")
                .add(0b01101111, "RLD")
                .add(0b10100000, "LDI,LDIR,LDD,LDDR", "", RD)
                .add(0b10100001, "CPI,CPIR,CPD,CPDR", "", RD)
                .add(0b10100010, "INI,INIR,IND,INDR", "", RD)
                .add(0b10100011, "OTI,OTIR,OTD,OTDR", "", RD);
        with(CB_OPCODES)
                .add(0b00000000, "RLC", "sss", SSS)
                .add(0b00001000, "RRC", "sss", SSS)
                .add(0b00010000, "RL", "sss", SSS)
                .add(0b00011000, "RR", "sss", SSS)
                .add(0b00100000, "SLA", "sss", SSS)
                .add(0b00101000, "SRA", "sss", SSS)
                .add(0b00110000, "SLL", "sss", SSS)
                .add(0b00111000, "SRL", "sss", SSS)
                .add(0b01000000, "BIT", "bit,sss", BIT, SSS)
                .add(0b10000000, "RES", "bit,sss", BIT, SSS)
                .add(0b11000000, "SET", "bit,sss", BIT,SSS);
    }
    private static Builder with(Opcode[] o) {
        return new Builder(o);
    }
    static class Builder {
        Opcode[] opcodes;
        Builder(Opcode[] opcodes) {
            assert(opcodes.length == 256);
            this.opcodes = opcodes;
        }
        Builder add(int baseOpcode, String mnemonic) {
            return add(baseOpcode, mnemonic, "");
        }
        Builder add(int baseOpcode, String mnemonic, String template, Flag... flags) {
            final Set<Flag> f = Set.of(flags);
            // Flags that don't blend with others
            if (f.contains(RP1) || f.contains(RP2)) {
                final String[] rp = { "BC", "DE", "HL", "SP" };
                final int size = f.contains(RP1) ? 2 : 4;
                for (int i=0; i<size; i++) {
                    int opcode = baseOpcode | i<<4;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("rp",rp[i]), flags);
                }
            }
            else if (f.contains(CC2) || f.contains(CC3)) {
                final String[] cc = { "NZ", "Z", "NC", "C", "PO", "PE", "P", "N" };
                final int size = f.contains(CC2) ? 4 : 8;
                for (int i=0; i<size; i++) {
                    int opcode = baseOpcode | i<<3;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("cc",cc[i]), flags);
                }
            }
            else if (f.contains(N3)) {
                for (int i=0; i<8; i++) {
                    int n = i<<3;
                    int opcode = baseOpcode | n;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("n",String.format("%02XH",n)), flags);
                }
            }
            else if (f.contains(N2)) {
                for (int i=0; i<4; i++) {
                    int opcode = baseOpcode | i<<3;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("n", Integer.toString(i)), flags);
                }
            }
            else if (f.contains(RD)) {
                final String[] mnemonics = mnemonic.split(",");
                assert mnemonics.length == 4;
                for (int i=0; i<4; i++) {
                    int opcode = baseOpcode | i<<3;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonics[i], template, flags);
                }
            }
            // These are flags which can combine, so need to be careful...
            else if (f.contains(DDD) && f.contains(SSS)) {
                final String[] regs = { "B", "C", "D", "E", "H", "L", "(HL)", "A" };
                for (int d=0; d<8; d++) {
                    String ddd= regs[d];
                    for (int s=0; s<8; s++) {
                        if (d==0b110 && s==0b110) {
                            // special case. LD (HL),(HL) is not a LD. This is instead HALT.
                            continue;
                        }
                        String sss= regs[s];
                        int opcode = baseOpcode | d<<3 | s;
                        assert opcodes[opcode] == null;
                        opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("ddd",ddd).replace("sss",sss), flags);
                    }
                }
            }
            else if (f.contains(ALU) && f.contains(SSS)) {
                final String[] regs = { "B", "C", "D", "E", "H", "L", "(HL)", "A" };
                final String[] mnemonics = mnemonic.split(",");
                assert(mnemonics.length == 8);
                for (int i=0; i<8; i++) {
                    for (int s=0; s<8; s++) {
                        String sss= regs[s];
                        int opcode = baseOpcode | i<<3 | s;
                        assert opcodes[opcode] == null;
                        opcodes[opcode] = new Opcode(opcode, mnemonics[i], template.replace("sss",sss), flags);
                    }
                }
            }
            else if (f.contains(BIT) && f.contains(SSS)) {
                final String[] regs = { "B", "C", "D", "E", "H", "L", "(HL)", "A" };
                for (int i=0; i<8; i++) {
                    final String bit = Integer.toString(i);
                    for (int s=0; s<8; s++) {
                        String sss= regs[s];
                        int opcode = baseOpcode | i<<3 | s;
                        assert opcodes[opcode] == null;
                        opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("bit",bit).replace("sss",sss), flags);
                    }
                }
            }
            else if (f.contains(DDD)) {
                final String[] regs = { "B", "C", "D", "E", "H", "L", "(HL)", "A" };
                for (int d=0; d<8; d++) {
                    String ddd= regs[d];
                    int opcode = baseOpcode | d<<3;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("ddd",ddd), flags);
                }
            }
            else if (f.contains(SSS)) {
                final String[] regs = { "B", "C", "D", "E", "H", "L", "(HL)", "A" };
                for (int s=0; s<8; s++) {
                    String sss= regs[s];
                    int opcode = baseOpcode | s;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("sss",sss), flags);
                }
            }
            else if (f.contains(ALU)) {
                final String[] mnemonics = mnemonic.split(",");
                assert(mnemonics.length == 8);
                for (int i=0; i<8; i++) {
                    int opcode = baseOpcode | i<<3;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonics[i], template, flags);
                }
            }
            else {
                assert opcodes[baseOpcode] == null;
                opcodes[baseOpcode] = new Opcode(baseOpcode, mnemonic, template, flags);
            }
            return this;
        }
    }
    enum Flag {
        RP1, RP2,
        DATLO, DATHI,
        DDD, SSS,
        DATA,
        OFFSET,
        ADDLO, ADDHI,
        CC3, CC2,
        PORT,
        ALU,
        N2, N3,
        PREFIX,
        OVERRIDE,
        RD,
        BIT;
    }
}
