package org.applecommander.disassembler.api;

import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;
import org.applecommander.disassembler.api.z80.InstructionSetZ80;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.io.*;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstructionSetTest {
    @ParameterizedTest(name = "6502[{0}] => {3}")
    @ArgumentsSource(InstructionSetProvider6502.class)
    public void test6502InstructionSet(int lineNumber, int address, byte[] code, String assembly) {
        test(InstructionSet6502.for6502(), address, code, assembly);
    }

    @ParameterizedTest(name = "65C02[{0}] => {3}")
    @ArgumentsSource(InstructionSetProvider65C02.class)
    public void test65C02InstructionSet(int lineNumber, int address, byte[] code, String assembly) {
        test(InstructionSet6502.for65C02(), address, code, assembly);
    }

    @ParameterizedTest(name = "6502X[{0}] => {3}")
    @ArgumentsSource(InstructionSetProvider6502X.class)
    public void test6502XInstructionSet(int lineNumber, int address, byte[] code, String assembly) {
        test(InstructionSet6502.for6502withIllegalInstructions(), address, code, assembly);
    }

    @ParameterizedTest(name = "SWEET16[{0}] => {3}")
    @ArgumentsSource(InstructionSetProviderSWEET16.class)
    public void testSWEET16InstructionSet(int lineNumber, int address, byte[] code, String assembly) {
        test(InstructionSetSWEET16.forSWEET16(), address, code, assembly);
    }

    @ParameterizedTest(name = "Z80[{0}] => {3}")
    @ArgumentsSource(InstructionSetProviderZ80.class)
    public void testZ80InstructionSet(int lineNumber, int address, byte[] code, String assembly) {
        test(InstructionSetZ80.forZ80(), address, code, assembly);
    }

    void test(InstructionSet instructionSet, int address, byte[] code, String assembly) {
        List<Instruction> instructions = Disassembler.with(code).use(instructionSet).startingAddress(address).decode();
        assertEquals(1, instructions.size());
        Instruction instruction = instructions.getFirst();
        assertArrayEquals(code, instruction.code());
        assertEquals(assembly, toAssembly(instruction));
    }

    String toAssembly(Instruction instruction) {
        StringBuilder builder = new StringBuilder(instruction.mnemonic());
        if (!instruction.operands().isEmpty()) {
            builder.append(' ');
            builder.append(instruction.operands().stream().map(Instruction.Operand::format).collect(Collectors.joining(",")));
        }
        return builder.toString();
    }

    static class InstructionSetProvider6502 extends InstructionSetProvider {
        InstructionSetProvider6502() {
            super("/6502.txt");
        }
    }
    static class InstructionSetProvider65C02 extends InstructionSetProvider {
        InstructionSetProvider65C02() {
            super("/6502.txt", "/65c02.txt");
        }
    }
    static class InstructionSetProvider6502X extends InstructionSetProvider {
        InstructionSetProvider6502X() {
            super("/6502.txt", "/6502X.txt");
        }
    }
    static class InstructionSetProviderSWEET16 extends InstructionSetProvider {
        InstructionSetProviderSWEET16() {
            super("/SWEET16.txt");
        }
    }
    static class InstructionSetProviderZ80 extends InstructionSetProvider {
        InstructionSetProviderZ80() {
            super("/Z80.txt");
        }
    }
    static class InstructionSetProvider implements ArgumentsProvider {
        static final Pattern HEX = Pattern.compile("^\\p{XDigit}\\p{XDigit}$", Pattern.CASE_INSENSITIVE);

        final String[] filenames;
        protected InstructionSetProvider(String... filenames) {
            this.filenames = filenames;
        }

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) throws Exception {
            Stream.Builder<Arguments> builder = Stream.builder();
            for (String filename : filenames) {
                InputStream inputStream = getClass().getResourceAsStream(filename);
                assert inputStream != null;

                int addr = 0;
                try (LineNumberReader reader = new LineNumberReader(new InputStreamReader(inputStream))) {
                    while (true) {
                        String line = reader.readLine();
                        if (line == null) break;
                        // Strip off comments (note: comments must be a full line)
                        if (line.startsWith("#")) {
                            continue;
                        }
                        line = line.trim();
                        // Skip empty lines
                        if (line.isBlank()) continue;
                        // Set default address (example "300:")
                        if (line.endsWith(":")) {
                            addr = Integer.parseInt(line.replace(":", ""), 16);
                            continue;
                        }
                        // Add test case (example "69 44      ADC #$44")
                        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
                        StringBuilder assembly = new StringBuilder();
                        boolean lookingForHex = true;
                        for (String part : line.split(" ")) {
                            if (lookingForHex && HEX.matcher(part).matches()) {
                                bytes.write(Integer.parseInt(part, 16));
                            } else {
                                lookingForHex = false;
                                // Using a "-" to separate opcodes that happen to be 2 letters which also are hex digits (aka SWEET16 "BC")
                                if ("-".equals(part)) continue;
                                if (!assembly.isEmpty()) assembly.append(' ');
                                assembly.append(part);
                            }
                        }
                        builder.add(Arguments.of(reader.getLineNumber(), addr, bytes.toByteArray(), assembly.toString()));
                    }
                }
            }
            return builder.build();
        }
    }
}
