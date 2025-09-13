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

public class InstructionSWEET16 implements Instruction {
    private final AddressModeSWEET16 addressMode;
    private final OpcodeSWEET16 opcode;
    private final int address;
    private final int register;
    private final byte[] code;
    private String operandLabel;
    
    InstructionSWEET16(AddressModeSWEET16 addressMode, OpcodeSWEET16 opcode, int register, int address, byte[] code) {
        this.addressMode = addressMode;
        this.opcode = opcode;
        this.register = register;
        this.address = address;
        this.code = code;
    }

    @Override
    public byte[] getBytes() {
        return code;
    }
    
    @Override
    public String getOpcodeMnemonic() {
        return opcode.getMnemonic();
    }

    @Override
    public boolean operandHasAddress() {
        return addressMode.isOperandAbsoluteAddress() || addressMode.isOperandRelativeAddress();
    }

    @Override
    public int getOperandValue() {
        switch (code.length) {
        case 3:
            return Byte.toUnsignedInt(code[1]) + Byte.toUnsignedInt(code[2])*256;
        case 2:
            if (addressMode.isOperandRelativeAddress()) {
                return (address + 2 + code[1]) & 0xffff;   // allow sign extension
            }
            else {
                return Byte.toUnsignedInt(code[1]);
            }
        default:
            return 0;
        }
    }
    
    @Override
    public void setOperandLabel(String label) {
        this.operandLabel = label;
    }

    @Override
    public String formatOperandWithValue() {
        String label = "-";
        if (addressMode.isOperandAbsoluteAddress() || addressMode.isOperandRelativeAddress()|| code.length == 3) {
            label = String.format("$%04X", getOperandValue());
        }
        else if (code.length == 2) {
            label = String.format("$%02X",getOperandValue());
        }
        return internalFormat(label);
    }
    
    @Override
    public String formatOperandWithLabel() {
        if (operandLabel == null) {
            return formatOperandWithValue();
        }
        return internalFormat(operandLabel);
    }

    String internalFormat(String label) {
        if (addressMode.doesOperandRequireRegister()) {
            if (code.length == 1) {
                return String.format(addressMode.getInstructionFormat(), getOpcodeMnemonic(), register);
            }
            return String.format(addressMode.getInstructionFormat(), getOpcodeMnemonic(), register, label);
        }
        else {
            if (code.length == 1) {
                return String.format(addressMode.getInstructionFormat(), getOpcodeMnemonic());
            }
            return String.format(addressMode.getInstructionFormat(), getOpcodeMnemonic(), label);
        }
    }
}
