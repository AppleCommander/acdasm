package io.github.applecommander.disassembler.api;

import io.github.applecommander.disassembler.api.mos6502.InstructionSet6502;
import io.github.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;
import io.github.applecommander.disassembler.api.z80.InstructionSetZ80;

import java.io.*;
import java.util.List;
import java.util.function.Function;

public class GenerateMarkdown {
    public static void main(String[] args) throws IOException {
        List<InstructionSet> instructionSets = List.of(
                InstructionSet6502.for6502(),
                InstructionSet6502.for6502withIllegalInstructions(),
                InstructionSet6502.for65C02(),
                InstructionSetSWEET16.forSWEET16(),
                InstructionSetZ80.forZ80()
            );

        for (InstructionSet instructionSet : instructionSets) {
            String filename = String.format("docs/%s.md", instructionSet.name());
            try (
                OutputStream out = new FileOutputStream(filename);
                PrintWriter pw = new PrintWriter(out)
            ) {
                createMarkdownTable(pw, instructionSet.name(), instructionSet::opcodeExample);
            }
        }
    }

    private static void createMarkdownTable(PrintWriter pw, String name, Function<Integer,String> fn) {
        pw.printf("# %s\n", name);
        // Header
        pw.print("| |");
        for (int x=0; x<16; x++) {
            pw.printf("_%1X |", x);
        }
        pw.println();
        // Set alignment
        pw.print("| :--- |");
        for (int x=0; x<16; x++) {
            pw.print(" :--- |");
        }
        pw.println();
        // Generate table
        for (int y = 0; y < 256; y += 16) {
            pw.printf("%1X_ |", y >> 4);
            for (int x = 0; x < 16; x++) {
                String text = fn.apply(y|x);
                pw.printf("%s |", text);
            }
            pw.println();
        }
        // Footers...
        pw.printf("\n\n(Automatically generated; do not change manually!)\n\n");
    }
}
