package org.applecommander.disassembler.api.pcode;

import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.Program;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.applecommander.disassembler.api.pcode.InstructionSetPCode.Flag.*;

public class InstructionSetPCode implements InstructionSet {
    public static InstructionSetPCode forApplePascal() {
        return new InstructionSetPCode();
    }

    // List of standard procedures.
    private static final String[] CSP_PROCS = {
            "", "NEW", "MVL", "MVR", "EXIT", "IDS", "TRS", "TIM",
            "FLC", "SCN", "TNC", "RND", "MRK", "RLS", "POT"
    };
    // List of type names. (2 = reals, 4 = strings, 6 = booleans, 8 = sets [opcodes listed as POWR though],
    // ... 10 = byte arrays, 12 = words)
    private static final String[] TYPE_NAMES = {
            "", "", "REAL", "", "STR", "", "BOOL", "", "POWR", "", "BYT", "", "WORD"
    };

    // Prevent construction
    private InstructionSetPCode() {}

    @Override
    public int defaultStartAddress() {
        // Relative to the file itself
        return 0;
    }

    @Override
    public int suggestedBytesPerInstruction() {
        return 8;
    }

    @Override
    public List<String> defaultLibraryLabels() {
        return List.of();
    }

    @Override
    public List<Instruction> decode(Program program) {
        List<Instruction> assembly = new ArrayList<>();
        Procedure procedure = new Procedure(program);
        while (procedure.hasMore()) {
            if (procedure.currentOffset() >= procedure.jumpTable()) {
                assembly.add(Instruction.at(procedure.currentAddress())
                        .mnemonic("J/T")
                        .opAddress("%s", "$%04X", procedure.readSelfRelativeW())
                        .code(procedure.bytesRead())
                        .get());
                continue;
            }

            Opcode opcode = OPCODES[procedure.readUB()];

            Instruction.Builder builder = Instruction.at(procedure.currentAddress());
            builder.mnemonic(opcode.mnemonic);
            // Note that we usually have only one, but sometimes we have DB,B or UB,B or UB,UB
            // ... so this makes us read it in the right order
            for (Flag flag : opcode.flags) {
                switch (flag) {
                    case UB, DB -> builder.opValue("%d", procedure.readUB());
                    case SB -> builder.opAddress("%s", "$%04X", procedure.readSBOffset());
                    case B -> builder.opValue("%d", procedure.readB());
                    case W -> builder.opValue("%d", procedure.readW());
                    case TYPE -> {
                        int t = procedure.readUB();
                        builder.mnemonic(String.format("%s%s", opcode.mnemonic, TYPE_NAMES[t]));
                        if (t == 10 || t == 12) {
                            builder.opValue("%d", procedure.readB());
                        }
                    }
                    case CSP -> {
                        int csp = procedure.readUB();
                        if (csp > 0 && csp < CSP_PROCS.length) {
                            builder.mnemonic(CSP_PROCS[csp]);
                        } else {
                            builder.opValue("%d", csp);
                        }
                    }
                    case LDC -> {
                        int ub = procedure.readUB();
                        builder.opValue("%d", ub);
                        procedure.alignToWord();
                        for (int i = 0; i < ub; i++) {
                            builder.opValue("%w", procedure.readW());
                        }
                    }
                    case LPA, LSA -> {
                        // Both are documented as <chars> but other docs suggest LPA may be bytes.
                        int ub = procedure.readUB();
                        StringBuilder sb = new StringBuilder();
                        for (int i = 0; i < ub; i++) {
                            sb.append((char) procedure.readUB());
                        }
                        builder.opValue("'%s'", sb.toString());
                    }
                    case XJP -> {
                        procedure.alignToWord();
                        int w1 = procedure.readW();
                        int w2 = procedure.readW();
                        builder.opValue("Range %d..%d", w1, w2);
                        procedure.readUB();                         // UJP per documentation
                        int w3addr = procedure.readSBOffset() + 6;  // adjusted for XJP itself
                        builder.opValue("UJP $%04X", w3addr);
                        for (int i = w1; i <= w2; i++) {
                            builder.opAddress("%s", "$%04X", procedure.readSelfRelativeW());
                        }
                    }
                    default -> throw new RuntimeException("Unexpected flag type: " + flag);
                }
            }
            // Catch stuff with constants
            opcode.impliedValue.ifPresent(n -> {
                builder.opValue("%d", n);
            });

            builder.code(procedure.bytesRead());
            assembly.add(builder.get());
        }
        return assembly;
    }

    @Override
    public List<OpcodeTable> opcodeTables() {
        return List.of(new OpcodeTablePCode());
    }

    private static class OpcodeTablePCode implements OpcodeTable {
        @Override
        public String name() {
            return "p-code";
        }

        @Override
        public String opcodeExample(int opcode) {
            Opcode op = OPCODES[opcode];
            if (op == null) return "-";

            return op.impliedValue
                     .map(integer -> String.format("%s %d", op.mnemonic, integer))
                     .orElseGet(() -> op.mnemonic);
        }
    }

    private static class Procedure {
        private final Program program;
        private int length;
        private int jumpTable;

        public Procedure(Program program) {
            this.program = program;
        }
        public boolean hasMore() {
            return program.hasMore();
        }
        public int jumpTable() {
            return program.length() + jumpTable + 8;    // account for attribute table
        }
        public void alignToWord() {
            if ((program.currentAddress() + length & 1) == 1) {
                length++;
            }
        }
        public int currentOffset() {
            return program.currentOffset();
        }
        public int currentAddress() {
            return program.currentAddress();
        }
        public byte[] bytesRead() {
            try {
                return program.read(length);
            } finally {
                length = 0;
            }
        }
        public int readUB() {
            return program.peekUnsignedByte(length++);
        }
        public int readSBOffset() {
            int sb = program.peekSignedByte(length++);
            if (sb < 0) {
                jumpTable = Math.min(jumpTable, sb);
                int offset = program.length() + sb + 8;    // account for attribute table
                int w = program.getUnsignedByte(offset) | program.getUnsignedByte(offset + 1) << 8;
                return program.baseAddress() + offset - w;
            } else {
                return program.currentAddress() + sb + 2;
            }
        }
        public int readW() {
            return readUB() | readUB() << 8;
        }
        public int readSelfRelativeW() {
            return program.currentAddress() + length - readW();
        }
        public int readB() {
            // Range 0..127
            int b = readUB();
            if (b > 127) {
                // Range 128..32768
                b = (b & 0x7f) << 8 | readUB();
            }
            return b;
        }
    }

    private static final Opcode[] OPCODES;
    private record Opcode(int opcode, String mnemonic, Optional<Integer> impliedValue, List<Flag> flags) {}

    static {
        OPCODES = new Opcode[256];
        opcodeRange(0, "SLDC", 0, 127);
        opcode(128, "ABI");
        opcode(129, "ABR");
        opcode(130, "ADI");
        opcode(131, "ADR");
        opcode(132, "LAND");
        opcode(133, "DIF");
        opcode(134, "DVI");
        opcode(135, "DVR");
        opcode(136, "CHK");
        opcode(137, "FLO");
        opcode(138, "FLT");
        opcode(139, "INN");
        opcode(140, "INT");
        opcode(141, "LOR");
        opcode(142, "MODI");
        opcode(143, "MPI");
        opcode(144, "MPR");
        opcode(145, "NGI");
        opcode(146, "NGR");
        opcode(147, "LNOT");
        opcode(148, "SRS");
        opcode(149, "SBI");
        opcode(150, "SBR");
        opcode(151, "SGS");
        opcode(152, "SQI");
        opcode(153, "SQR");
        opcode(154, "STO");
        opcode(155, "IXS");
        opcode(156, "UNI");
        opcode(157, "LDE", UB, B);
        opcode(158, "CSP", CSP);
        opcode(159, "LDCN");
        opcode(160, "ADJ", UB);
        opcode(161, "FJP", SB);
        opcode(162, "INC", B);
        opcode(163, "IND", B);
        opcode(164, "IXA", B);
        opcode(165, "LAO", B);
        opcode(166, "LSA", LSA);
        opcode(167, "LAE", UB, B);
        opcode(168, "MOV", B);
        opcode(169, "LDO", B);
        opcode(170, "SAS", UB);
        opcode(171, "SRO", B);
        opcode(172, "XJP", XJP);
        opcode(173, "RNP", DB);
        opcode(174, "CIP", UB);
        opcode(175, "EQU", TYPE);
        opcode(176, "GEQ", TYPE);
        opcode(177, "GRT", TYPE);
        opcode(178, "LDA", DB, B);
        opcode(179, "LDC", LDC);
        opcode(180, "LEQ", TYPE);
        opcode(181, "LES", TYPE);
        opcode(183, "NEQ", TYPE);
        opcode(182, "LOD", DB, B);
        opcode(184, "STR", DB, B);
        opcode(185, "UJP", SB);
        opcode(186, "LDP");
        opcode(187, "STP");
        opcode(188, "LDM", UB);
        opcode(189, "STM", UB);
        opcode(190, "LDB");
        opcode(191, "STB");
        opcode(192, "IXP", UB, UB);     // UB1 and UB2
        opcode(193, "RBP", DB);
        opcode(194, "CBP", UB);
        opcode(195, "EQUI");
        opcode(196, "GEQI");
        opcode(197, "GRTI");
        opcode(198, "LLA", B);
        opcode(199, "LDCI", W);
        opcode(200, "LEQI");
        opcode(201, "LESI");
        opcode(202, "LDL", B);
        opcode(203, "NEWI");
        opcode(204, "STL", B);
        opcode(205, "CXP", UB, UB);     // UB1 and UB2
        opcode(206, "CLP", UB);
        opcode(207, "CGP", UB);
        opcode(208, "LPA", LPA);
        opcode(209, "STE", UB, B);
        opcode(211, "EFJ", SB);
        opcode(212, "NFJ", SB);
        opcode(213, "BPT", B);
        opcode(214, "XIT");
        opcode(215, "NOP");
        opcodeRange(1, "SLDL", 216, 231);
        opcodeRange(1, "SLDO", 232, 247);
        opcodeRange(0, "SIND", 248, 255);
    }
    static void opcodeRange(int firstValue, String mnemonic, int firstOpcode, int lastOpcode) {
        for (int opcode = firstOpcode; opcode <= lastOpcode; opcode++) {
            assert OPCODES[opcode] == null;
            OPCODES[opcode] = new Opcode(opcode, mnemonic, Optional.of(opcode - firstOpcode + firstValue), List.of());
        }
    }
    static void opcode(int opcode, String mnemonic, Flag... flags) {
        assert OPCODES[opcode] == null;
        OPCODES[opcode] = new Opcode(opcode, mnemonic, Optional.empty(), List.of(flags));
    }
    /** p-code flags indicating and operands. Mix of documented flags per documentation and made up flags. */
    enum Flag {
        /** Unsigned Byte. High order byte of parameter is implicitly zero. */
        UB,
        /** Signed Byte. High order byte is sign extension of bit 7. Note: only used for jumps, so results in address. */
        SB,
        /** Don't care byte. Can be treated as SB or UB, as value is always in the range 0..127. */
        DB,
        /**
         * Big. This parameter is one byte long when used to represent values in the range 0..127,
         * and is two bytes long when representing values in the range 128..32767. If the first byte
         * is in 0..127, the high byte of hte parameter is implicitly zero. Otherwise, bit 7 of the
         * first byte is cleared, and it is used as the high order byte of the parameter. The second
         * byte is used as the low order byte.
         */
        B,
        /** Word. The next two bytes, low byte first, give the parameter value. */
        W,
        /**
         * Data type. UB follows opcode:
         *  2 = real
         *  4 = string
         *  6 = boolean
         *  8 = set
         *  10 = byte array - additional B follows to indicate number of bytes
         *  12 = word - additional B follows to indicate number of words
         */
        TYPE,
        /** Call Standard Procedure. Following byte indicates procedure number. */
        CSP,
        /** UB,block. Word-aligned block of UB words in reverse word order. */
        LDC,
        /** UB,bytes. UB is length of packed array. */
        LPA,
        /** UB,chars. UB is length of string. */
        LSA,
        /**
         * Case jump: XJP W1,W2,W3,case-table.
         * W1 is word-aligned, and is the minimum index of the table.
         * W2 is the maximum index.
         * W3 is an unconditional jump instruction past the table.
         * The case table is <code>(W2 - W1 + 1)</code> words long, and
         * contains self-relative locations.
         * <p/>
         * If TOS, the actual index, is not in the range W1..W2, then point IPC
         * at W3. Otherwise, use <code>TOS - W1</code> as an index into the case
         * table, and set IPC to the byte address <code>casetable[tos-W1]</code>
         * minus the contents of <code>casetable[tos-W1]</code>.
         */
        XJP
    }
}
