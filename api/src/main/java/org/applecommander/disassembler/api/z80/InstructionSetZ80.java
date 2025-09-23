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
package org.applecommander.disassembler.api.z80;

import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.Program;

import java.util.List;
import java.util.Set;

import static org.applecommander.disassembler.api.z80.InstructionSetZ80.Flag.*;

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
    public List<String> defaultLibraryLabels() {
        // TBD. No explicit labels defined.
        return List.of();
    }

    @Override
    public int suggestedBytesPerInstruction() {
        return 5;
    }

    @Override
    public Instruction decode(Program program) {
        int addr = program.currentAddress();
        Instruction.Builder builder = Instruction.at(addr);

        int length = 1;
        int b = program.peekUnsignedByte();
        Opcode op = ROOT_OPCODES[b];
        boolean ix = false;
        boolean iy = false;
        boolean hasDisplacement = false;
        // Overrides first
        if (op.flags.contains(OVERRIDE)) {
            ix = op.opcode == 0xdd;
            iy = op.opcode == 0xfd;
            b = program.peekUnsignedByte(length);
            op = ROOT_OPCODES[b];
            length++;
        }
        // Setup for IX+override and IY+override (manual, uncertain of nice way)
        if ((ix|iy) && (b == 0x36 || b == 0xcb)) {
            // (DD|FD) (36|CB) <displacement> <opcode>
            hasDisplacement = true;
            length++;
        }
        // Alternate prefixes next
        if (op.flags.contains(PREFIX)) {
            b = program.peekUnsignedByte(length);
            if (op.opcode() == 0xed) {
                op = ED_OPCODES[b];
            }
            else if (op.opcode() == 0xcb) {
                op = CB_OPCODES[b];
            }
            length++;
        }
        builder.mnemonic(op.mnemonic);
        // Operands - figure out extra bytes
        int operandValue = 0;
        if ((op.flags.contains(DATLO) && op.flags.contains(DATHI))
                || (op.flags.contains(ADDLO) && op.flags.contains(ADDHI))) {
            int b1 = program.peekUnsignedByte(length);
            int b2 = program.peekUnsignedByte(length + 1);
            operandValue = b1 | b2 << 8;
            length += 2;
        }
        if (op.flags.contains(DATA) || op.flags.contains(PORT)) {
            operandValue = program.peekUnsignedByte(length);
            length += 1;
        }
        if (op.flags.contains(OFFSET)) {
            operandValue = addr + program.peekUnsignedByte(length) + 2;
            length += 1;
        }
        // Operands - add into builder
        for (String operandFmt : op.fmts) {
            // Handle IX / IY
            if (ix || iy) {
                String reg = ix ? "IX" : "IY";
                if (operandFmt.contains("(HL)") && hasDisplacement) {
                    int displacement = program.peekUnsignedByte(2);
                    operandFmt = operandFmt.replace("(HL)", String.format("(%s+%02XH)", reg, displacement));
                } else if (operandFmt.contains("(HL)") && b == 0xe9) {
                    // JP (IX) and JP (IY) are special
                    operandFmt = operandFmt.replace("(HL)", String.format("(%s)", reg));
                } else if (operandFmt.contains("(HL)")) {
                    int displacement = program.peekUnsignedByte(length);
                    operandFmt = operandFmt.replace("(HL)", String.format("(%s+%02XH)", reg, displacement));
                    length++;
                } else if (operandFmt.contains("HL")) {
                    operandFmt = operandFmt.replace("HL", reg);
                }
            }
            // Setup the operand
            if (operandFmt.contains("data") && op.flags.contains(DATLO)) {
                builder.opValue(operandFmt.replace("data", "%04XH"), operandValue);
            }
            else if (operandFmt.contains("add")) {
                builder.opAddress(operandFmt.replace("add", "%s"), "%04XH", operandValue);
            }
            else if (operandFmt.contains("port")) {
                builder.opValue(operandFmt.replace("port", "%02XH"), operandValue);
            }
            else if (operandFmt.contains("data") && op.flags.contains(DATA)) {
                builder.opValue(operandFmt.replace("data", "%02XH"), operandValue);
            }
            else if (operandFmt.contains("offset")) {
                builder.opAddress(operandFmt.replace("offset", "%s"), "%04XH", operandValue);
            }
            else if (!operandFmt.isEmpty()) {
                builder.opValue(operandFmt);
            }
        }
        //
        //return new InstructionZ80(addr, op.mnemonic, operandFmt, operandValue, program.read(length));
        builder.code(program.read(length));
        return builder.get();
    }

    @Override
    public List<OpcodeTable> opcodeTables() {
        return List.of(
                new OpcodeTableZ80("Z80 Opcodes", ROOT_OPCODES),
                new OpcodeTableZ80("'ED' Opcodes", ED_OPCODES),
                new OpcodeTableZ80("'CB' Opcodes", CB_OPCODES)
            );
    }

    private static class OpcodeTableZ80 implements OpcodeTable {
        private final String name;
        private final Opcode[] opcodes;

        private OpcodeTableZ80(String name, Opcode[] opcodes) {
            this.name = name;
            this.opcodes = opcodes;
        }

        @Override
        public String name() {
            return name;
        }

        @Override
        public String opcodeExample(int op) {
            Opcode opcode = opcodes[op];
            if (opcode == null) {
                return "-";
            }
            String fmt = String.join(",",opcode.fmts)
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
    }

    record Opcode(int opcode, String mnemonic, String[] fmts, Set<Flag> flags) {
        public Opcode(int opcode, String mnemonic, String fmts, Flag... flags) {
            this(opcode, mnemonic, fmts.split(","), Set.of(flags));
        }
    }

    private static final Opcode[] ROOT_OPCODES = new Opcode[256];
    private static final Opcode[] ED_OPCODES = new Opcode[256];
    private static final Opcode[] CB_OPCODES = new Opcode[256];
    static {
        with(ROOT_OPCODES)
                .add(0b00000000, "NOP")
                .add(0b00000001, "LD", "rp,data", RP2SP, DATLO, DATHI)
                .add(0b00000010, "LD", "(rp),A", RP1)
                .add(0b00000011, "INC", "rp", RP2SP)
                .add(0b00000100, "INC", "ddd", DDD)
                .add(0b00000101, "DEC", "ddd", DDD)
                .add(0b00000110, "LD", "ddd,data", DDD, DATA)
                .add(0b00000111, "RLCA")
                .add(0b00001001, "ADD", "HL,rp", RP2SP)
                .add(0b00001010, "LD", "A,(rp)", RP1)
                .add(0b00001011, "DEC", "rp", RP2SP)
                .add(0b00001000, "EX", "AF,AF'")
                .add(0b00001111, "RRCA")
                .add(0b00010000, "DJNZ", "offset", OFFSET)
                .add(0b00010111, "RLA")
                .add(0b00011000, "JR", "offset", OFFSET)
                .add(0b00011111, "RRA")
                .add(0b00100000, "JR", "cc,offset", CC2, OFFSET)
                .add(0b00100010, "LD", "(add),HL", ADDLO, ADDHI)
                .add(0b00100111, "DAA")
                .add(0b00101010, "LD", "HL,(add)", ADDLO, ADDHI)
                .add(0b00101111, "CPL")
                .add(0b00110010, "LD", "(add),A", ADDLO, ADDHI)
                .add(0b00110111, "SCF")
                .add(0b00111010, "LD", "A,(add)", ADDLO, ADDHI)
                .add(0b00111111, "CCF")
                .add(0b01000000, "LD", "ddd,sss", DDD, SSS)
                .add(0b01110110, "HALT")
                .add(0b10000000, "ADD,ADC,SUB,SBC,AND,XOR,OR,CP", "A,sss", SSS, ALU)
                .add(0b11000000, "RET", "cc", CC3)
                .add(0b11000001, "POP", "rp", RP2AF)
                .add(0b11000010, "JP", "cc,add", CC3, ADDLO, ADDHI)
                .add(0b11000011, "JP", "add", ADDLO, ADDHI)
                .add(0b11000100, "CALL", "cc,add", CC3, ADDLO, ADDHI)
                .add(0b11000101, "PUSH", "rp", RP2AF)
                .add(0b11000110, "ADD,ADC,SUB,SBC,AND,XOR,OR,CP", "A,data", ALU, DATA)
                .add(0b11000111, "RST", "n", N3)
                .add(0b11001001, "RET")
                .add(0b11001011, "CB", "", PREFIX)
                .add(0b11001101, "CALL", "add", ADDLO, ADDHI)
                .add(0b11010011, "OUT", "(port),A", PORT)
                .add(0b11011001, "EXX")
                .add(0b11011011, "IN", "A,(port)", PORT)
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
                .add(0b01000010, "SBC", "HL,rp", RP2SP)
                .add(0b01000011, "LD", "(add),rp", ADDLO, ADDHI, RP2SP)
                .add(0b01000100, "NEG")
                .add(0b01000101, "RETN")
                .add(0b01000110, "IM", "0")
                .add(0b01010110, "IM", "1")
                .add(0b01011110, "IM", "2")
                .add(0b01000111, "LD", "I,A")
                .add(0b01001010, "ADC", "HL,rp", RP2SP)
                .add(0b01001011, "LD", "rp,(add)", ADDLO, ADDHI, RP2SP)
                .add(0b01001101, "RETI")
                .add(0b01001111, "LD", "R,A")
                .add(0b01010111, "LD", "A,I")
                .add(0b01011111, "LD", "A,R")
                .add(0b01100111, "RRD")
                .add(0b01101111, "RLD")
                .add(0b10100000, "LDI,LDD,LDIR,LDDR", "", RD)
                .add(0b10100001, "CPI,CPD,CPIR,CPDR", "", RD)
                .add(0b10100010, "INI,IND,INIR,INDR", "", RD)
                .add(0b10100011, "OUTI,OUTD,OTIR,OTDR", "", RD);
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
            if (f.contains(RP1) || f.contains(RP2SP)) {
                final String[] rp = { "BC", "DE", "HL", "SP" };
                final int size = f.contains(RP1) ? 2 : 4;
                for (int i=0; i<size; i++) {
                    int opcode = baseOpcode | i<<4;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("rp",rp[i]), flags);
                }
            }
            else if (f.contains(RP2AF)) {
                final String[] rp = { "BC", "DE", "HL", "AF" };
                final int size = f.contains(RP1) ? 2 : 4;
                for (int i=0; i<size; i++) {
                    int opcode = baseOpcode | i<<4;
                    assert opcodes[opcode] == null;
                    opcodes[opcode] = new Opcode(opcode, mnemonic, template.replace("rp",rp[i]), flags);
                }
            }
            else if (f.contains(CC2) || f.contains(CC3)) {
                final String[] cc = { "NZ", "Z", "NC", "C", "PO", "PE", "P", "M" };
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
        RP1, RP2SP, RP2AF,
        DATLO, DATHI,
        DDD, SSS,
        DATA,
        OFFSET,
        ADDLO, ADDHI,
        CC3, CC2,
        PORT,
        ALU,
        N3,
        PREFIX,
        OVERRIDE,
        RD,
        BIT
    }
}
