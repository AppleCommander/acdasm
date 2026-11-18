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
    private final List<Integer> entryPoints;
    private final BitSet dataFlags;
    private final List<Integer> labelAddresses;
    private int startAddress;
    private byte[] code;

    Disassembler(int startAddress, int bytesToSkip, int bytesToDecode, byte[] code,
                 InstructionSet instructionSet, List<Integer> entryPoints) {
        this.startAddress = startAddress;
        this.bytesToSkip = bytesToSkip;
        this.bytesToDecode = bytesToDecode;
        this.code = code;
        this.instructionSet = instructionSet;
        this.entryPoints = Collections.unmodifiableList(entryPoints);
        this.dataFlags = new BitSet(code.length);
        this.labelAddresses = new ArrayList<>();
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

        // Assume all is code
        dataFlags.clear(0, code.length);
        Program program = new Program(code,startAddress);
        if (!entryPoints.isEmpty()) {
            // If we have entry points, we need to scan the program and identify data versus code.
            prescan(program);
            program.currentAddress(program.baseAddress());
        }
        List<Instruction> assembly = new ArrayList<>();
        while (program.hasMore()) {
            if (dataFlags.get(program.currentOffset())) {
                assembly.add(parseData(program));
            } else {
                assembly.add(instructionSet.decodeOne(program));
            }
        }

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

    public Instruction parseData(Program program) {
        int currentAddress = program.currentAddress();
        int nextLabel = labelAddresses.stream().filter(n -> n > currentAddress)
                .findFirst().orElse(program.lastAddress()+1);
        int bytes = nextLabel - currentAddress;
        int n = 0;
        int ch = Byte.toUnsignedInt(program.peek(n)) & 0x7f;
        if (ch >= 0x20 && ch < 0x7f) {
            // Assume we have a string. Scan ahead to see how far we get.
            final int highBit = Byte.toUnsignedInt(program.peek(n)) & 0x80;
            StringBuilder sb = new StringBuilder();
            while (ch >= 0x20 && ch < 0x7f && n < bytes) {
                int priorHighBit = Byte.toUnsignedInt(program.peek(n)) & 0x80;
                sb.append((char)ch);
                n++;
                // Exit if we get to end of range -or- if the high bit switched
                if (n == bytes || priorHighBit != highBit) break;
                ch = Byte.toUnsignedInt(program.peek(n)) & 0x7f;
            }
            return Instruction.at(currentAddress)
                    .code(program.read(n))
                    .mnemonic(".string")
                    .opValue("'%s'", sb.toString())
                    .get();
        } else {
            while (ch < 0x20 || ch == 0x7f && n < bytes) {
                n++;
                if (n == bytes) break;
                ch = Byte.toUnsignedInt(program.peek(n)) & 0x7f;
            }
            byte[] data = program.read(n);
            return Instruction.at(currentAddress)
                    .code(data)
                    .mnemonic(".hex")
                    .opValue(HexFormat.of().formatHex(data))
                    .get();
        }
    }

    public void prescan(Program program) {
        // Assume all is data and we'll figure out the code.
        dataFlags.set(0, code.length);
        labelAddresses.clear();

        Set<Integer> completedAddresses = new HashSet<>();
        List<Integer> codeAddresses = new ArrayList<>();
        if (entryPoints.isEmpty()) {
            codeAddresses.add(program.baseAddress());
        } else {
            codeAddresses.addAll(entryPoints);
        }
        while (!codeAddresses.isEmpty()) {
            // Only review an address once.
            int address = codeAddresses.removeFirst();
            if (completedAddresses.contains(address)) {
                continue;
            }
            completedAddresses.add(address);
            program.currentAddress(address);
            // Run until we see a terminal instruction.
            while (program.hasMore()) {
                int offset = program.currentOffset();
                completedAddresses.add(program.currentAddress());
                Instruction inst = instructionSet.decodeOne(program);
                dataFlags.clear(offset, offset+inst.code().length);
                Optional<Instruction.Operand> addressRef = inst.addressRef();
                // Handle (simple) code branches; nothing indirect
                if (addressRef.isPresent()) {
                    Instruction.Operand operand = addressRef.get();
                    if (operand.target().isPresent()) {
                        Instruction.Target target = operand.target().get();
                        int opAddr = target.address();
                        if (program.inRange(opAddr) && !labelAddresses.contains(opAddr)) {
                            labelAddresses.add(opAddr);
                        }
                        if (target.type() == Instruction.Type.CODE) {
                            codeAddresses.add(opAddr);
                        }
                    }
                }
                // If code does NOT continue, stop processing.
                if (inst.terminal()) {
                    break;
                }
            }
        }
        labelAddresses.sort(Integer::compare);
    }
    
    public static class Builder {
        private final Set<String> sections = new HashSet<>();
        private final byte[] code;
        private int startAddress;
        private int bytesToSkip;
        private int bytesToDecode;
        private InstructionSet instructionSet;
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
                    instructionSet, entryPoints);
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
