package org.applecommander.disassembler.api;

import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.ArgumentsProvider;
import org.junit.jupiter.params.provider.ArgumentsSource;
import org.junit.jupiter.params.support.ParameterDeclarations;

import java.io.BufferedReader;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;

public class InstructionSetTest {
    @ParameterizedTest(name = "6502[{index}] => {2}")
    @ArgumentsSource(InstructionSetProvider6502.class)
    public void test6502InstructionSet(int address, byte[] code, String assembly) {
        test(InstructionSet6502.for6502(), address, code, assembly);
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

    static class InstructionSetProvider6502 implements ArgumentsProvider {
        static final Pattern HEX = Pattern.compile("^\\p{XDigit}\\p{XDigit}$", Pattern.CASE_INSENSITIVE);

        @Override
        public Stream<? extends Arguments> provideArguments(ParameterDeclarations parameters, ExtensionContext context) throws Exception {
            InputStream inputStream = getClass().getResourceAsStream("/6502.txt");
            assert inputStream != null;

            Stream.Builder<Arguments> builder = Stream.builder();
            int addr = 0;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
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
                    for (String part : line.split(" ")) {
                        if (HEX.matcher(part).matches()) {
                            bytes.write(Integer.parseInt(part, 16));
                        }
                        else {
                            if (!assembly.isEmpty()) assembly.append(' ');
                            assembly.append(part);
                        }
                    }
                    builder.add(Arguments.of(addr, bytes.toByteArray(), assembly.toString()));
                }
            }
            return builder.build();
        }
    }
}
