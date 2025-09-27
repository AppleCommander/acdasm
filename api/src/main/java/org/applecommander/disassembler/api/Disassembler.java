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
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.ini4j.Ini;
import org.ini4j.Profile.Section;

import org.applecommander.disassembler.api.mos6502.InstructionSet6502;

/**
 * Disassembler is the primary interface to disassemble a program.
 * <p/>
 * An overview:
 * <pre>
 * {@code
 * Map<Integer,String> labels = new HashMap<>();
 * List<Instruction> assembly = Disassembler.with(code)
 *         .startingAddress(startAddress)
 *         .bytesToSkip(offset)
 *         .bytesToDecode(length)
 *         .use(instructionSet)
 *         .section(libraries)
 *         .decode(labels);
 * }
 * </pre>
 */
public class Disassembler {
    private static final Ini ini = new Ini();
    static {
        try (InputStream is = Disassembler.class.getResourceAsStream("/addresses.ini")) {
            ini.load(is);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    /** Returns the label groups defined in the {@code addresses.ini} file. */
    public static Set<String> labelGroups() {
        return ini.keySet();
    }

    private int startAddress;
    private int bytesToSkip;
    private int bytesToDecode;
    private byte[] code;
    private InstructionSet instructionSet;

    /** Initiate the disassembly. */
    public static Builder with(byte[] code) {
        return new Builder(code);
    }
    
    private List<Instruction> decode(Map<Integer,String> labels) {
        // Create a subset of the original code and adjust starting address accordingly
        if (bytesToSkip > 0 || bytesToDecode > 0) {
            byte[] dest = new byte[bytesToDecode == 0 ? code.length - bytesToSkip : bytesToDecode];
            System.arraycopy(code, bytesToSkip, dest, 0, dest.length);
            code = dest;
            startAddress+= bytesToSkip;
        }

        Program program = new Program(code,startAddress);
        List<Instruction> assembly = instructionSet.decode(program);

        // Gather all the instructions and identify all target addresses
        assembly.forEach(instruction -> {
            instruction.addressRef().flatMap(Instruction.Operand::address).ifPresent(address -> {
                if ((address >= startAddress) && (address < startAddress + code.length)) {
                    labels.computeIfAbsent(address, addr -> String.format("L%04X", addr));
                }
            });
        });

        return assembly;
    }
    
    public static class Builder {
        private final Set<String> sections = new HashSet<>();
        private final Disassembler disassembler = new Disassembler();
        
        public Builder(byte[] code) {
            disassembler.startAddress = 0x300;
            disassembler.code = code;
            disassembler.instructionSet = InstructionSet6502.for6502();
        }
        /** Decode without preserving labels. */
        public List<Instruction> decode() {
            return this.decode(new HashMap<>());
        }
        /** Decode and preserve labels. Will also include any label groups requested. */
        public List<Instruction> decode(Map<Integer,String> labels) {
            assert labels != null;
            assert disassembler.instructionSet != null;
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
        /** The starting address for the disassembly. */
        public Builder startingAddress(int address) {
            disassembler.startAddress = address;
            return this;
        }
        /** Any bytes to skip in the program. Defaults to 0. */
        public Builder bytesToSkip(int skip) {
            disassembler.bytesToSkip = skip;
            return this;
        }
        /** How much of the program to disassemble. Defaults to entire program. */
        public Builder bytesToDecode(int length) {
            disassembler.bytesToDecode = length;
            return this;
        }
        /** Select the instruction set. Required. */
        public Builder use(InstructionSet instructionSet) {
            disassembler.instructionSet = instructionSet;
            return this;
        }
        /** Add any label groups requested. Allows "All" and "None". */
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
}
