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
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.Program;

import java.util.ArrayList;
import java.util.List;

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
    public List<Instruction> decode(Program program) {
        List<Instruction> assembly = new ArrayList<>();
        while (program.hasMore()) {
            assembly.add(decodeOne(program));
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

        Instruction.Builder builder = Instruction.at(currentAddress)
                .code(program.read(addressMode.getInstructionLength()))
                .mnemonic(opcode.getMnemonic());
        // Notes: ZZZ{1,2,3} are length of the invalid opcode. Picked most simple representation; not meant to be
        //        technically correct.
        switch (addressMode) {
            case ACC, IMP, ZZZ1 -> {
            }
            case ABS, REL, ZZZ3 -> builder.opAddress("%s", "$%04X", value);
            case ABSX -> builder.opAddress("%s", "$%04X", value).opValue("X");
            case ABSY -> builder.opAddress("%s", "$%04X", value).opValue("Y");
            case IMM -> builder.opValue("#$%02X", value);
            case INDABS -> builder.opAddress("(%s)", "$%04X", value);
            case INDABSX -> builder.opAddress("(%s", "$%04X", value).opValue("X)");
            case INDZP -> builder.opAddress("(%s)", "$%02X", value);
            case INDZPX -> builder.opAddress("(%s", "$%02X", value).opValue("X)");
            case INDZPY -> builder.opAddress("(%s)", "$%02X", value).opValue("Y");
            case ZP, ZZZ2 -> builder.opAddress("%s", "$%02X", value);
            case ZPX -> builder.opAddress("%s", "$%02X", value).opValue("X");
            case ZPY -> builder.opAddress("%s", "$%02X", value).opValue("Y");
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
