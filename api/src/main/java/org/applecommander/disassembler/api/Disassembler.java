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

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import org.applecommander.disassembler.api.mos6502.InstructionSet6502;

public class Disassembler {
    private static final Ini ini = new Ini();
    static {
        try (InputStream is = Disassembler.class.getResourceAsStream("/addresses.ini")) {
            ini.load(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private int startAddress;
    private int bytesToSkip;
    private byte[] code;
    private InstructionSet instructionSet;

    public static Builder with(byte[] code) {
        return new Builder(code);
    }
    
    private List<Instruction> decode(Map<Integer,String> labels) {
        List<Instruction.Builder> assembly = new ArrayList<>();
        Program program = new Program(code,startAddress);

        // 1st pass: Gather all the instruction builders and identify all target addresses
        while (program.hasMore()) {
            Instruction.Builder builder = null;
            if (program.currentOffset() < bytesToSkip) {
                builder = Instruction.at(program.currentAddress())
                        .mnemonic("---")
                        .code(program.read(1));
            }
            else {
                builder = instructionSet.decode(program);
            }
            assembly.add(builder);

            builder.addressRef().flatMap(Instruction.OpBuilder::address).ifPresent(address -> {
                if ((address >= startAddress) && (address < startAddress + code.length)) {
                    labels.computeIfAbsent(address, addr -> String.format("L%04X", addr));
                }
            });
        }

        return assembly.stream().map(Instruction.Builder::get).toList();
    }
    
    public static class Builder {
        private final Set<String> sections = new HashSet<>();
        private final Disassembler disassembler = new Disassembler();
        
        public Builder(byte[] code) {
            disassembler.startAddress = 0x300;
            disassembler.code = code;
            disassembler.instructionSet = InstructionSet6502.for6502();
        }
        public List<Instruction> decode() {
            return this.decode(new HashMap<>());
        }
        public List<Instruction> decode(Map<Integer,String> labels) {
            assert labels != null;
            // merge in all selected sections
            for (String name : sections) {
                Section section = ini.get(name);
                if (section == null) {
                    throw new RuntimeException(String.format("Section '%s' not defined.", name));
                }
                for (Map.Entry<String,String> entry : section.entrySet()) {
                    Optional<Integer> address = convert(entry.getValue());
                    address.ifPresent(integer -> labels.putIfAbsent(integer, entry.getKey()));
                }
            }
            
            return disassembler.decode(labels);
        }
        
        public Builder startingAddress(int address) {
            disassembler.startAddress = address;
            return this;
        }
        public Builder bytesToSkip(int skip) {
            disassembler.bytesToSkip = skip;
            return this;
        }
        public Builder use(InstructionSet instructionSet) {
            disassembler.instructionSet = instructionSet;
            return this;
        }
        public Builder use6502() {
            disassembler.instructionSet = InstructionSet6502.for6502();
            return this;
        }
        public Builder use6502WithIllegalOpcodes() {
            disassembler.instructionSet = InstructionSet6502.for6502withIllegalInstructions();
            return this;
        }
        public Builder use65C02() {
            disassembler.instructionSet = InstructionSet6502.for65C02();
            return this;
        }
        
        public Builder section(List<String> names) {
            if (names != null) {
                this.sections.addAll(names);
            }
            return this;
        }
    }

    /** Add support for "$801" and "0x801" instead of just decimal like 2049. */
    public static Optional<Integer> convert(String value) {
        if (value == null) {
            return Optional.empty();
        } else if (value.startsWith("$")) {
            return Optional.of(Integer.valueOf(value.substring(1), 16));
        } else if (value.startsWith("0x") || value.startsWith("0X")) {
            return Optional.of(Integer.valueOf(value.substring(2), 16));
        } else {
            return Optional.of(Integer.valueOf(value));
        }
    }
    
    public static Set<String> sections() {
        return ini.keySet();
    }
}
