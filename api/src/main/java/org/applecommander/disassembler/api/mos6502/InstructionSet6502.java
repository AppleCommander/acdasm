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

import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.Instruction.Operand;
import org.applecommander.disassembler.api.Instruction.Target;
import org.applecommander.disassembler.api.Instruction.Type;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.Program;

import java.util.*;

public class InstructionSet6502 implements InstructionSet {
    public static InstructionSet6502 for6502() {
        return new InstructionSet6502("6502", AddressMode6502.MOS6502, Opcode6502.MOS6502);
    }
    public static InstructionSet6502 for6502withIllegalInstructions() {
        return new InstructionSet6502("6502X", AddressMode6502.MOS6502, Opcode6502.MOS6502_WITH_ILLEGAL);
    }
    public static InstructionSet6502 for65C02() {
        return new InstructionSet6502("65C02", AddressMode6502.WDC65C02, Opcode6502.WDC65C02);
    }
    
    private final AddressMode6502[] addressModes;
    private final Opcode6502[] opcodes;
    private final String name;
    private BitSet dataFlags;
    private final List<Integer> labelAddresses = new ArrayList<>();

    private InstructionSet6502(String name, AddressMode6502[] addressModes, Opcode6502[] opcodes) {
        this.name = name;
        this.addressModes = addressModes;
        this.opcodes = opcodes;
    }

    @Override
    public Defaults defaults() {
        return Defaults.builder()
                .startAddress(0x300)
                .libraryLabels("All")
                .bytesPerInstruction(3)
                .get();
    }

    @Override
    public void prescan(Program program, List<Integer> entryPoints) {
        // Assume it's all data.
        dataFlags = new BitSet(program.length());
        dataFlags.set(0, program.length());

        Set<Integer> completedAddresses = new HashSet<>();
        List<Integer> codeAddresses = new ArrayList<>();
        if (entryPoints.isEmpty()) {
            codeAddresses.add(program.baseAddress());
        } else {
            codeAddresses.addAll(entryPoints);
        }
        while (!codeAddresses.isEmpty()) {
            // Only review an address once.
            int address = codeAddresses.removeFirst();
            if (completedAddresses.contains(address)) {
                continue;
            }
            completedAddresses.add(address);
            program.currentAddress(address);
            // Run until we see an (obvious) exit of RTS or RTI.
            while (program.hasMore()) {
                int offset = program.currentOffset();
                completedAddresses.add(program.currentAddress());
                Instruction inst = decodeOne(program);
                dataFlags.clear(offset, offset+inst.code().length);
                Optional<Operand> addressRef = inst.addressRef();
                // Handle (simple) code branches; nothing indirect
                if (addressRef.isPresent()) {
                    Operand operand = addressRef.get();
                    if (operand.target().isPresent()) {
                        Target target = operand.target().get();
                        int opAddr = target.address();
                        if (program.inRange(opAddr) && !labelAddresses.contains(opAddr)) {
                            labelAddresses.add(opAddr);
                        }
                        if (target.type() == Type.CODE) {
                            codeAddresses.add(opAddr);
                        }
                    }
                }
                // If code does NOT continue, stop processing.
                // TODO on Instruction yet
                final Set<String> controlTransfer = Set.of("JMP", "BRA", "RTS", "RTI");
                if (controlTransfer.contains(inst.mnemonic())) {
                    break;
                }
            }
        }
        labelAddresses.sort(Integer::compare);
    }

    @Override
    public List<Instruction> decode(Program program) {
        List<Instruction> assembly = new ArrayList<>();
        while (program.hasMore()) {
            int currentAddress = program.currentAddress();
            if (dataFlags != null && dataFlags.get(program.currentOffset())) {
                int nextLabel = labelAddresses.stream().filter(n -> n > currentAddress)
                        .findFirst().orElse(program.lastAddress()+1);
                int bytes = nextLabel - currentAddress;
                byte[] data = program.read(bytes);
                for (int i=0; i<data.length; i++) {
                    StringBuilder sb = new StringBuilder();
                    final int highBit = Byte.toUnsignedInt(data[i]) & 0x80;
                    for (int j=i; j<data.length; j++) {
                        int ch = Byte.toUnsignedInt(data[j]) & 0x7f;
                        if (ch < 0x20 || ch == 0x7f) break;
                        sb.append((char)ch);
                        // Keep high bit setting the same - but we include one assuming it's the end
                        if ((Byte.toUnsignedInt(data[j]) & 0x80) != highBit) break;
                    }
                    if (sb.length() > 1) {
                        assembly.add(Instruction.at(currentAddress + i)
                                .code(Arrays.copyOfRange(data, i, i+sb.length()))
                                .mnemonic(".string")
                                .opValue("'%s'", sb.toString())
                                .get());
                        i += sb.length()-1;
                    }
                    else {
                        int length = Math.min(16, data.length-i);
                        assembly.add(Instruction.at(currentAddress+i)
                                .code(Arrays.copyOfRange(data, i, i+length))
                                .mnemonic(".hex")
                                .opValue(HexFormat.of().formatHex(data, i, i+length))
                                .get());
                        i += length-1;
                    }
                }
            }
            else {
                assembly.add(decodeOne(program));
            }
        }
        return assembly;
    }

    /**
     * Single instruction decoding has been extracted to support the 6502/SWEET16 switching mode.
     */
    public Instruction decodeOne(Program program) {
        int op = program.peekUnsignedByte();

        AddressMode6502 addressMode = addressModes[op];
        Opcode6502 opcode = opcodes[op];

        int currentAddress = program.currentAddress();
        int value = switch (addressMode.getInstructionLength()) {
            case 3 -> program.peekUnsignedShort(1);
            case 2 -> {
                if (addressMode.isOperandRelativeAddress()) {
                    yield (currentAddress + 2 + program.peekSignedByte(1)) & 0xffff;   // allow sign extension
                } else {
                    yield program.peekUnsignedByte(1);
                }

            }
            default -> 0;
        };

        // Partial determination of target type for ABS and REL branches.
        // Other types of branching like "JMP ($1234,X)" are DATA since $1234 is not the actual address.
        Type type = switch (opcode) {
            case BCC, BCS, BEQ, BMI, BNE, BPL, BRA, BVC, BVS, JSR, JMP -> Type.CODE;
            default -> Type.DATA;
        };

        Instruction.Builder builder = Instruction.at(currentAddress)
                .code(program.read(addressMode.getInstructionLength()))
                .mnemonic(opcode.getMnemonic());
        // Notes: ZZZ{1,2,3} are length of the invalid opcode. Picked most simple representation; not meant to be
        //        technically correct.
        switch (addressMode) {
            case ACC, IMP, ZZZ1 -> {
            }
            case ABS, REL, ZZZ3 -> builder.opAddress("%s", "$%04X", value, type);
            case ABSX -> builder.opAddress("%s", "$%04X", value, Type.DATA).opValue("X");
            case ABSY -> builder.opAddress("%s", "$%04X", value, Type.DATA).opValue("Y");
            case IMM -> builder.opValue("#$%02X", value);
            case INDABS -> builder.opAddress("(%s)", "$%04X", value, Type.DATA);
            case INDABSX -> builder.opAddress("(%s", "$%04X", value, Type.DATA).opValue("X)");
            case INDZP -> builder.opAddress("(%s)", "$%02X", value, Type.DATA);
            case INDZPX -> builder.opAddress("(%s", "$%02X", value, Type.DATA).opValue("X)");
            case INDZPY -> builder.opAddress("(%s)", "$%02X", value, Type.DATA).opValue("Y");
            case ZP, ZZZ2 -> builder.opAddress("%s", "$%02X", value, Type.DATA);
            case ZPX -> builder.opAddress("%s", "$%02X", value, Type.DATA).opValue("X");
            case ZPY -> builder.opAddress("%s", "$%02X", value, Type.DATA).opValue("Y");
        }
        return builder.get();
    }

    @Override
    public List<OpcodeTable> opcodeTables() {
        return List.of(new OpcodeTable6502());
    }

    private class OpcodeTable6502 implements OpcodeTable {
        @Override
        public String name() {
            return name;
        }

        @Override
        public String opcodeExample(int op) {
            AddressMode6502 addressMode = addressModes[op];
            Opcode6502 opcode = opcodes[op];
            if (opcode == Opcode6502.ZZZ) {
                return "-";
            }

            String name = opcode.getMnemonic();
            return switch (addressMode) {
                case ACC, IMP, ZZZ1 -> name;
                case ABS, REL, ZZZ3 -> String.format("%s ADDR", name);
                case ABSX -> String.format("%s ADDR,X", name);
                case ABSY -> String.format("%s ADDR,Y", name);
                case IMM -> String.format("%s #VALUE", name);
                case INDABS -> String.format("%s (ADDR)", name);
                case INDABSX -> String.format("%s (ADDR,X)", name);
                case INDZP -> String.format("%s (ZP)", name);
                case INDZPX -> String.format("%s (ZP,X)", name);
                case INDZPY -> String.format("%s (ZP),Y", name);
                case ZP -> String.format("%s ZP", name);
                case ZPX -> String.format("%s ZP,X", name);
                case ZPY -> String.format("%s ZP,Y", name);
                case ZZZ2 -> String.format("%s VALUE", name);
            };
        }
    }
}
