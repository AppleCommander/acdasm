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
package io.github.applecommander.disassembler.api;

import java.util.Optional;

public class SkippedInstruction implements Instruction {
    public static Instruction from(Program program) {
        int currentAddress = program.currentAddress();  // Need capture before read
        byte[] code = program.read(1);
        return new SkippedInstruction(currentAddress, code);
    }
    
    private int address;
    private byte[] code;
    private String addressLabel;
    
    SkippedInstruction(int address, byte[] code) {
        this.address = address;
        this.code = code;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public byte[] getBytes() {
        return code;
    }
    
    @Override
    public Optional<String> getAddressLabel() {
        return Optional.ofNullable(addressLabel);
    }
    
    @Override
    public void setAddressLabel(String label) {
        this.addressLabel = label;
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