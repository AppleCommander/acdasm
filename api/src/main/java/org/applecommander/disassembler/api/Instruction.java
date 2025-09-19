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

public record Instruction(int address, Optional<String> addressLabel, byte[] code,
                          String mnemonic, List<Operand> operands) {

    public record Operand(String opFmt, String value, Optional<Integer> address, Optional<String> addressLabel) {
        public String format() {
            return String.format(opFmt, addressLabel.orElse(value));
        }
    }

    public static Builder at(int address) {
        return new Builder(address);
    }
    public static class Builder {
        private final int address;
        private String addressLabel = null;
        private byte[] code = new byte[0];
        private String mnemonic = "";
        private final List<OpBuilder> opBuilders = new ArrayList<>();

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
        public Builder addressLabel(String addressLabel) {
            this.addressLabel = addressLabel;
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
            opBuilders.add(new OpBuilder(opFmt).address(fmt, address));
            return this;
        }
        public Builder opValue(String fmt, Object... values) {
            opBuilders.add(new OpBuilder("%s").value(fmt, values));
            return this;
        }
        public Optional<OpBuilder> addressRef() {
            for (OpBuilder operand : opBuilders) {
                if (operand.address().isPresent()) {
                    return Optional.of(operand);
                }
            }
            return Optional.empty();
        }
        public Instruction get() {
            List<Operand> operands = opBuilders.stream().map(OpBuilder::get).toList();
            return new Instruction(address, Optional.ofNullable(addressLabel), code, mnemonic, operands);
        }
    }

    public static class OpBuilder {
        private final String opFmt;
        private String value;
        private Integer address;
        private String addressLabel;

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
        public OpBuilder addressLabel(String label) {
            this.addressLabel = label;
            return this;
        }
        public Operand get() {
            return new Operand(opFmt, value, address(), Optional.ofNullable(addressLabel));
        }
    }
}
