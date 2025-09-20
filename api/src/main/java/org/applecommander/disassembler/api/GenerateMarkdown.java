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

import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;
import org.applecommander.disassembler.api.z80.InstructionSetZ80;

import java.io.*;
import java.util.List;
import java.util.Map;

import static org.applecommander.disassembler.api.InstructionSet.OpcodeTable;

public class GenerateMarkdown {
    public static void main(String[] args) throws IOException {
        Map<String,InstructionSet> instructionSets = Map.of(
                "6502", InstructionSet6502.for6502(),
                "6502X", InstructionSet6502.for6502withIllegalInstructions(),
                "65C02", InstructionSet6502.for65C02(),
                "SWEET16", InstructionSetSWEET16.forSWEET16(),
                "Z80", InstructionSetZ80.forZ80()
            );

        for (Map.Entry<String,InstructionSet> entry : instructionSets.entrySet()) {
            String filename = String.format("docs/%s.md", entry.getKey());
            try (
                OutputStream out = new FileOutputStream(filename);
                PrintWriter pw = new PrintWriter(out)
            ) {
                createMarkdownTable(pw, entry.getValue().opcodeTables());
            }
        }
    }

    private static void createMarkdownTable(PrintWriter pw, List<OpcodeTable> opcodeTables) {
        for (OpcodeTable opcodeTable : opcodeTables) {
            pw.printf("# %s\n", opcodeTable.name());
            // Header
            pw.print("| |");
            for (int x = 0; x < 16; x++) {
                pw.printf(" _%1X |", x);
            }
            pw.println();
            // Set alignment
            pw.print("| :--- |");
            for (int x = 0; x < 16; x++) {
                pw.print(" :--- |");
            }
            pw.println();
            // Generate table
            for (int y = 0; y < 256; y += 16) {
                pw.printf(" %1X_ |", y >> 4);
                for (int x = 0; x < 16; x++) {
                    String text = opcodeTable.opcodeExample(y | x);
                    pw.printf(" %s |", text);
                }
                pw.println();
            }
        }
        // Footers...
        pw.printf("\n\n(Automatically generated; do not change manually!)\n\n");
    }
}
