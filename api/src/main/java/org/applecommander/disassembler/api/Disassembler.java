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

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.dataformat.toml.TomlMapper;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.*;

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
    private static final Map<String,Map<String,Integer>> ADDRESSES = new HashMap<>();
    static {
        TomlMapper mapper = new TomlMapper();
        try (InputStream is = Disassembler.class.getResourceAsStream("/addresses.toml")) {
            TypeReference<Map<String,Map<String,Integer>>> typeRef = new TypeReference<>() {};
            ADDRESSES.putAll(mapper.readValue(is, typeRef));
            assert !ADDRESSES.isEmpty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }
    /** Returns the label groups defined in the {@code addresses.yaml} file. */
    public static Set<String> labelGroups() {
        return ADDRESSES.keySet();
    }

    private final int bytesToSkip;
    private final int bytesToDecode;
    private final InstructionSet instructionSet;
    private final boolean prescanCode;
    private final List<Integer> entryPoints;
    private int startAddress;
    private byte[] code;

    Disassembler(int startAddress, int bytesToSkip, int bytesToDecode, byte[] code,
                 InstructionSet instructionSet, boolean prescanCode, List<Integer> entryPoints) {
        this.startAddress = startAddress;
        this.bytesToSkip = bytesToSkip;
        this.bytesToDecode = bytesToDecode;
        this.code = code;
        this.instructionSet = instructionSet;
        this.prescanCode = prescanCode;
        this.entryPoints = Collections.unmodifiableList(entryPoints);
    }

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
        if (prescanCode) {
            instructionSet.prescan(program, entryPoints);
            program.currentAddress(program.baseAddress());
        }
        List<Instruction> assembly = instructionSet.decode(program);

        // Gather all the instructions and identify all target addresses
        assembly.forEach(instruction -> {
            instruction.addressRef().flatMap(Instruction.Operand::target).ifPresent(target -> {
                int address = target.address();
                if (program.inRange(address)) {
                    labels.computeIfAbsent(address, addr -> String.format("L%04X", addr));
                }
            });
        });

        return assembly;
    }
    
    public static class Builder {
        private final Set<String> sections = new HashSet<>();
        private final byte[] code;
        private int startAddress;
        private int bytesToSkip;
        private int bytesToDecode;
        private InstructionSet instructionSet;
        private boolean prescanCode;
        private List<Integer> entryPoints = new ArrayList<>();

        public Builder(byte[] code) {
            this.startAddress = 0x300;
            this.code = code;
            this.instructionSet = InstructionSet6502.for6502();
        }
        /** Decode without preserving labels. */
        public List<Instruction> decode() {
            return this.decode(new HashMap<>());
        }
        /** Decode and preserve labels. Will also include any label groups requested. */
        public List<Instruction> decode(Map<Integer,String> labels) {
            assert labels != null;
            assert this.instructionSet != null;
            // merge in all selected sections
            for (String name : sections) {
                Map<String,Integer> section = ADDRESSES.get(name);
                if (section == null) {
                    throw new RuntimeException(String.format("Section '%s' not defined.", name));
                }
                section.forEach((label, address) -> {
                    labels.putIfAbsent(address, label);
                });
            }

            Disassembler disassembler = new Disassembler(startAddress, bytesToSkip, bytesToDecode, code,
                    instructionSet, prescanCode, entryPoints);
            return disassembler.decode(labels);
        }
        /** The starting address for the disassembly. */
        public Builder startingAddress(int address) {
            this.startAddress = address;
            return this;
        }
        /** Any bytes to skip in the program. Defaults to 0. */
        public Builder bytesToSkip(int skip) {
            this.bytesToSkip = skip;
            return this;
        }
        /** How much of the program to disassemble. Defaults to entire program. */
        public Builder bytesToDecode(int length) {
            this.bytesToDecode = length;
            return this;
        }
        /** Select the instruction set. Required. */
        public Builder use(InstructionSet instructionSet) {
            this.instructionSet = instructionSet;
            return this;
        }
        /** Enable the prescan capability in an effort to identify data versus code. */
        public Builder prescan(boolean prescanFlag) {
            this.prescanCode = prescanFlag;
            return this;
        }
        public Builder entryPoint(int address) {
            this.entryPoints.add(address);
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
}
