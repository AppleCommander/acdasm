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
package org.applecommander.disassembler.api;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

/**
 * An instruction represents one decoded lines. It potentially could also be a "directive" (not really, but useful
 * for humans); for example the switching instruction set uses ".6502" and ".SWEET16" to indicate mode switches.
 *
 * @param address The starting address for this instruction.
 * @param code The raw bytes representing the instruction.
 * @param mnemonic The opcode mnemonic.
 * @param operands List of operands, assumed to be joined with ",".
 * @param description An optional description given to the opcode.
 * @param terminal This instruction terminates the routine. That is, code execution does not go past this instruction.
 */
public record Instruction(int address, byte[] code, String mnemonic, List<Operand> operands,
                          Optional<String> description, boolean terminal) {

    public Optional<Operand> addressRef() {
        for (Operand operand : operands) {
            if (operand.target().isPresent()) {
                return Optional.of(operand);
            }
        }
        return Optional.empty();
    }

    /**
     * An operand represents a single operand.
     *
     * @param opFmt Operand format string. This should be with "%s" and include any formatting.
     *              Example: Immediate addressing would be "#%s".
     * @param value The formatted value. It will be combined with the operand format string, unless a label is requested.
     * @param target An optional address. The disassembler uses this to identify labels.
     */
    public record Operand(String opFmt, String value, Optional<Target> target) {
        /** Format the operand with the given value. */
        public String format() {
            return String.format(opFmt, value);
        }
        /** Format the operand but use the given label instead of the value. */
        public String format(String label) {
            return String.format(opFmt, label);
        }
    }

    /**
     * A target captures the target and the type of target.
     *
     * @param address The target this operand works on.
     * @param type The type of target.
     */
    public record Target(int address, Type type) {
    }

    /**
     * The type of target. Code applies to actual target, not indirect addresses (so <tt>JMP ($0300)</tt>
     * has a target that is considered _data_.)
     */
    public enum Type {
        CODE,
        DATA
    }

    /**
     * Initiate construction of an instruction.
     * <p/>
     * For example (this is fabricated but also gives an idea):
     * <pre>
     * {@code
     * int operandValue = program.peekUnsignedByte(1);
     * Instruction inst = Instruction.at(program.currentAddress())
     *                               .code(program.read(2))
     *                               .mnemonic("LDA")
     *                               .opValue("#%s", "%d", operandValue)
     *                               .get();
     * }
     * </pre>
     */
    public static Builder at(int address) {
        return new Builder(address);
    }
    public static class Builder {
        private final int address;
        private byte[] code = new byte[0];
        private String mnemonic = "";
        private final List<Operand> operands = new ArrayList<>();
        private String description;
        private boolean terminal = false;

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
        public Builder terminal() {
            this.terminal = true;
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
        public Builder opAddress(String opFmt, String fmt, int address, Type type) {
            operands.add(new OpBuilder(opFmt).address(fmt, address, type).get());
            return this;
        }
        public Builder opValue(String fmt, Object... values) {
            operands.add(new OpBuilder("%s").value(fmt, values).get());
            return this;
        }
        public Builder description(String description) {
            assert description != null;
            this.description = description;
            return this;
        }
        public Instruction get() {
            return new Instruction(address, code, mnemonic, operands, Optional.ofNullable(description), terminal);
        }
    }

    public static class OpBuilder {
        private final String opFmt;
        private String value;
        private Target target;

        private OpBuilder(String opFmt) {
            this.opFmt = opFmt;
        }
        public OpBuilder value(String fmt, Object... values) {
            this.value = String.format(fmt, values);
            return this;
        }
        public OpBuilder address(String fmt, int address, Type type) {
            Objects.requireNonNull(type);
            this.value = String.format(fmt, address);
            this.target = new Target(address, type);
            return this;
        }
        public Optional<Target> address() {
            return Optional.ofNullable(target);
        }
        public Operand get() {
            return new Operand(opFmt, value, Optional.ofNullable(target));
        }
    }
}
