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
package org.applecommander.disassembler.api.sweet16;

import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.Program;

import java.util.List;

public class InstructionSetSWEET16 implements InstructionSet {
    public static InstructionSetSWEET16 forSWEET16() {
        return new InstructionSetSWEET16();
    }

    private InstructionSetSWEET16() {
        // Prevent construction
    }

    @Override
    public List<String> defaultLibraryLabels() {
        return List.of("All");
    }

    @Override
    public int defaultStartAddress() {
        return 0x300;
    }

    @Override
    public int suggestedBytesPerInstruction() {
        return 3;
    }

    @Override
    public Instruction decode(Program program) {
        int op = program.peek();
        int low = op & 0x0f;
        int high = (op & 0xf0) >> 4;

        AddressModeSWEET16 addressMode;
        OpcodeSWEET16 opcode;
        if (high == 0) {
            opcode = OpcodeSWEET16.NON_REGISTER_OPS[low];
            addressMode = AddressModeSWEET16.NON_REGISTER_OPS[low];
        }
        else {
            opcode = OpcodeSWEET16.REGISTER_OPS[high];
            addressMode = AddressModeSWEET16.REGISTER_OPS[high];
        }

        if (opcode == OpcodeSWEET16.ZZZ) {
            addressMode = AddressModeSWEET16.IMP;
        }

        int currentAddress = program.currentAddress();  // Need capture before read
        byte[] code = program.read(addressMode.getInstructionLength());
        int value = switch (code.length) {
            case 3 -> Byte.toUnsignedInt(code[1]) + Byte.toUnsignedInt(code[2])*256;
            case 2 -> {
                if (addressMode.isOperandRelativeAddress()) {
                    yield (currentAddress + 2 + code[1]) & 0xffff;   // allow sign extension
                }
                else {
                    yield Byte.toUnsignedInt(code[1]);
                }
            }
            default -> 0;
        };

        Instruction.Builder builder = Instruction.at(currentAddress)
                .code(code)
                .mnemonic(opcode.getMnemonic());
        switch(addressMode) {
            case CON -> builder.opValue("R%d", low).opValue("#$%04X", value);
            case DIR -> builder.opValue("R%d", low);
            case IND -> builder.opValue("@R%d", low);
            case BRA -> builder.opAddress("%s", "$%04X", value);
            case IMP -> {}
        }
        return builder.get();
    }

    @Override
    public List<OpcodeTable> opcodeTables() {
        return List.of(new OpcodeTableSWEET16());
    }

    private static class OpcodeTableSWEET16 implements OpcodeTable {
        @Override
        public String name() {
            return "SWEET16";
        }

        @Override
        public String opcodeExample(int op) {
            int low = op & 0x0f;
            int high = (op & 0xf0) >> 4;

            AddressModeSWEET16 addressMode;
            OpcodeSWEET16 opcode;
            if (high == 0) {
                opcode = OpcodeSWEET16.NON_REGISTER_OPS[low];
                addressMode = AddressModeSWEET16.NON_REGISTER_OPS[low];
            }
            else {
                opcode = OpcodeSWEET16.REGISTER_OPS[high];
                addressMode = AddressModeSWEET16.REGISTER_OPS[high];
            }
            if (opcode == OpcodeSWEET16.ZZZ) {
                addressMode = AddressModeSWEET16.IMP;
            }

            StringBuilder sb = new StringBuilder();
            sb.append(opcode.getMnemonic());
            sb.append(" ");
            sb.append(switch (addressMode) {
                case CON -> String.format("R%X,#VALUE", low);
                case BRA -> "ADDR";
                case DIR -> String.format("R%X", low);
                case IND -> String.format("@R%X", low);
                case IMP -> "";
            });
            return sb.toString();
        }
    }
}
