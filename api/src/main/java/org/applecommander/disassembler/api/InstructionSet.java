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

import java.util.Collections;
import java.util.List;

/**
 * InstructionSet is the core mechanism to decode a given program.
 */
public interface InstructionSet {
    /** Provides default values that are useful when setting up the Disassembler. */
    Defaults defaults();
    /** Decodes a program into a set of instructions. */
    Instruction decodeOne(Program program);
    /**
     * Generates an "opcode table" that can be used to generate documentation.
     * Each table is 256 bytes; most have 1 but Z80 has 3.
     * Optional.
     */
    List<OpcodeTable> opcodeTables();

    /** OpcodeTable provides the mechanism to create string representations of every opcode. */
    interface OpcodeTable {
        /** Name to use for the filename and heading. */
        String name();
        /** For a given opcode, create a string representation. For example 6502, <code>$20</code> could be "JSR ADDRESS". */
        String opcodeExample(int opcode);
    }

    /**
     * Provides default values that are valid for the InstructionSet.
     *
     * @param startAddress Common starting address for an application. Could be somewhat arbitrary.
     * @param libraryLabels A default set of library labels. Commonly is "All" or "None".
     * @param bytesPerInstruction Number of bytes commonly used in an expression. The disassembler does wrap after, so nothing is lost.
     * @param includeDescription Some instruction sets (p-code in particular) include instruction descriptions that may be useful.
     */
    record Defaults(int startAddress, List<String> libraryLabels, int bytesPerInstruction, boolean includeDescription) {
        /**
         * Initiate construction of a Defaults object.
         * <p/>
         * For example:
         * <pre>
         * {@code
         * return Defaults.builder()
         *                .startAddress(0x300)
         *                .libraryLabels("All")
         *                .bytesPerInstruction(3)
         *                .get();
         * }
         * </pre>
         */
        public static Builder builder() {
            return new Builder();
        }
        public static class Builder {
            private int startAddress;
            private List<String> libraryLabels = Collections.emptyList();
            private int bytesPerInstruction;
            private boolean includeDescription;

            public Builder startAddress(int startAddress) {
                this.startAddress = startAddress;
                return this;
            }
            public Builder libraryLabels(String... libraryLabels) {
                this.libraryLabels = List.of(libraryLabels);
                return this;
            }
            public Builder bytesPerInstruction(int bytesPerInstruction) {
                this.bytesPerInstruction = bytesPerInstruction;
                return this;
            }
            public Builder includeDescription(boolean includeDescription) {
                this.includeDescription = includeDescription;
                return this;
            }
            public Defaults get() {
                return new Defaults(startAddress, libraryLabels, bytesPerInstruction, includeDescription);
            }
        }
    }
}
