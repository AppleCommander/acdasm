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

public class InstructionZ80 implements Instruction {
    private final String mnemonic;
    private final int address;
    private final byte[] code;
    private String operandLabel;
    private final String operandFmt;
    private final int operandValue;

    public InstructionZ80(int address, String mnemonic, String operandFmt, int operandValue, byte[] code) {
        this.address = address;
        this.mnemonic = mnemonic;
        this.operandFmt = operandFmt;
        this.operandValue = operandValue;
        this.code = code;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public byte[] getBytes() {
        return code;
    }

    @Override
    public String getOpcodeMnemonic() {
        return mnemonic;
    }

    @Override
    public boolean operandHasAddress() {
        return operandValue > 0;
    }

    @Override
    public int getOperandValue() {
        return operandValue;
    }

    @Override
    public void setOperandLabel(String label) {
        this.operandLabel = label;
    }

    @Override
    public String formatOperandWithValue() {
        return String.format("%s %s", mnemonic, operandFmt);
    }

    @Override
    public String formatOperandWithLabel() {
        if (operandLabel == null) {
            return formatOperandWithValue();
        }
        return String.format("%s %s", mnemonic, operandFmt.replaceAll("\\X{4}H", operandLabel));
    }
}
