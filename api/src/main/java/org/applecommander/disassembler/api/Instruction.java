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

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public record Instruction(int address, byte[] code, String mnemonic, List<Operand> operands) {
    public Optional<Operand> addressRef() {
        for (Operand operand : operands) {
            if (operand.address().isPresent()) {
                return Optional.of(operand);
            }
        }
        return Optional.empty();
    }


    public record Operand(String opFmt, String value, Optional<Integer> address) {
        public String format() {
            return String.format(opFmt, value);
        }
        public String format(String label) {
            return String.format(opFmt, label);
        }
    }

    public static Builder at(int address) {
        return new Builder(address);
    }
    public static class Builder {
        private final int address;
        private byte[] code = new byte[0];
        private String mnemonic = "";
        private final List<Operand> operands = new ArrayList<>();

        private Builder(int address) {
            this.address = address;
        }
        public int address() {
            return this.address;
        }
        public Builder code(byte[] code) {
            assert code != null;
            this.code = code;
            return this;
        }
        public Builder mnemonic(String mnemonic) {
            assert mnemonic != null;
            this.mnemonic = mnemonic;
            return this;
        }
        public String mnemonic() {
            return this.mnemonic;
        }
        public Builder opAddress(String opFmt, String fmt, int address) {
            operands.add(new OpBuilder(opFmt).address(fmt, address).get());
            return this;
        }
        public Builder opValue(String fmt, Object... values) {
            operands.add(new OpBuilder("%s").value(fmt, values).get());
            return this;
        }
        public Optional<Operand> addressRef() {
            for (Operand operand : operands) {
                if (operand.address().isPresent()) {
                    return Optional.of(operand);
                }
            }
            return Optional.empty();
        }
        public Instruction get() {
            return new Instruction(address, code, mnemonic, operands);
        }
    }

    public static class OpBuilder {
        private final String opFmt;
        private String value;
        private Integer address;

        private OpBuilder(String opFmt) {
            this.opFmt = opFmt;
        }
        public OpBuilder value(String fmt, Object... values) {
            this.value = String.format(fmt, values);
            return this;
        }
        public OpBuilder address(String fmt, int address) {
            this.value = String.format(fmt, address);
            this.address = address;
            return this;
        }
        public Optional<Integer> address() {
            return Optional.ofNullable(address);
        }
        public Operand get() {
            return new Operand(opFmt, value, Optional.ofNullable(address));
        }
    }
}
