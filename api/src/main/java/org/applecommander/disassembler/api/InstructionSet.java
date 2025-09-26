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

import java.util.Collections;
import java.util.List;

public interface InstructionSet {
    Defaults defaults();
    List<Instruction> decode(Program program);
    List<OpcodeTable> opcodeTables();

    interface OpcodeTable {
        String name();
        String opcodeExample(int opcode);
    }

    record Defaults(int startAddress, List<String> libraryLabels, int bytesPerInstruction, boolean includeDescription) {
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
