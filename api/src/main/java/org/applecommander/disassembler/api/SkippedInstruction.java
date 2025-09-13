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
package org.applecommander.disassembler.api;

public class SkippedInstruction implements Instruction {
    public static Instruction from(Program program) {
        byte[] code = program.read(1);
        return new SkippedInstruction(code);
    }
    
    private final byte[] code;

    SkippedInstruction(byte[] code) {
        this.code = code;
    }

    @Override
    public byte[] getBytes() {
        return code;
    }

    @Override
    public String getOpcodeMnemonic() {
        return "---";
    }

    @Override
    public boolean operandHasAddress() {
        return false;
    }

    @Override
    public int getOperandValue() {
        return 0;
    }
    
    @Override
    public void setOperandLabel(String label) {
        // Unused, ignoring.
    }

    @Override
    public String formatOperandWithValue() {
        return getOpcodeMnemonic();
    }

    @Override
    public String formatOperandWithLabel() {
        return getOpcodeMnemonic();
    }
}