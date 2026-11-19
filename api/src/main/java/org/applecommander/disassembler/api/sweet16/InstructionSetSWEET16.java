/*
 * Copyright 2026  Robert Greene
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.applecommander.disassembler.api.sweet16;

import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.Instruction.Type;
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
    public Defaults defaults() {
        return Defaults.builder()
                .startAddress(0x300)
                .libraryLabels("All")
                .bytesPerInstruction(3)
                .get();
    }

    /**
     * Single instruction decoding has been extracted to support the 6502/SWEET16 switching mode.
     */
    @Override
    public Instruction decodeOne(Program program) {
        int op = program.peekUnsignedByte();
        int low = op & 0x0f;
        int high = (op & 0xf0) >> 4;

        AddressModeSWEET16 addressMode;
        OpcodeSWEET16 opcode;
        if (high == 0) {
            opcode = OpcodeSWEET16.NON_REGISTER_OPS[low];
            addressMode = AddressModeSWEET16.NON_REGISTER_OPS[low];
        } else {
            opcode = OpcodeSWEET16.REGISTER_OPS[high];
            addressMode = AddressModeSWEET16.REGISTER_OPS[high];
        }

        if (opcode == OpcodeSWEET16.ZZZ) {
            addressMode = AddressModeSWEET16.IMP;
        }

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
        switch (opcode) {
            case RTN, BR, RS -> builder.terminal();
            default -> {}
        }
        switch (addressMode) {
            case CON -> builder.opValue("R%d", low).opValue("#$%04X", value);
            case DIR -> builder.opValue("R%d", low);
            case IND -> builder.opValue("@R%d", low);
            case BRA -> builder.opAddress("%s", "$%04X", value, Type.CODE);
            case IMP -> {
            }
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

            return opcode.getMnemonic() +
                    switch (addressMode) {
                        case CON -> String.format(" R%X,#VALUE", low);
                        case BRA -> " ADDR";
                        case DIR -> String.format(" R%X", low);
                        case IND -> String.format(" @R%X", low);
                        case IMP -> "";
                    };
        }
    }
}
