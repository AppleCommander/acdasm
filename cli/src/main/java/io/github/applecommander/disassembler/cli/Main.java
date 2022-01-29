package io.github.applecommander.disassembler.cli;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import io.github.applecommander.disassembler.api.AddressMode;
import io.github.applecommander.disassembler.api.Instruction;

public class Main {
    public static void main(String[] args) throws IOException {
        Path path = Path.of(args[0]); 
        final byte[] code = Files.readAllBytes(path);
        
        int addr = 0;   // FIXME faking it for now!
        while (addr < code.length) {
            byte opcode = code[addr];
            //Instruction inst = Instruction.MOS6502[Byte.toUnsignedInt(opcode)];
            Instruction inst = Instruction.MOS6502_WITH_ILLEGAL[Byte.toUnsignedInt(opcode)];
            AddressMode addressMode = AddressMode.MOS6502[Byte.toUnsignedInt(opcode)];

            // Note that this makes any partial instruction at the end be displayed as invalid!
            if (inst == Instruction.ZZZ || addr + addressMode.getLength() >= code.length) {
                emit(addr, "???", AddressMode.IMP, new byte[] { opcode });
                addr+= 1;
            }
            else {
                byte[] x = Arrays.copyOfRange(code, addr, addr+addressMode.getLength());
                emit(addr, inst.getMnemonic(), addressMode, x);
                addr+= addressMode.getLength();
            }
        }
    }
    
    public static void emit(int addr, String mnemonic, AddressMode addressMode, byte[] code) {
        final int[] multiplier = { 0, 1, 256 };
        System.out.printf("%04X- ", addr);
        int arg = 0;
        for (int i=0; i<3; i++) {
            if (i >= code.length) {
                System.out.printf("   ");
            } else {
                System.out.printf("%02X ", code[i]);
                arg += (Byte.toUnsignedInt(code[i]) * multiplier[i]);
            }
        }
        System.out.printf(" ");
        System.out.printf("%s\n", addressMode.format(mnemonic, arg));
    }
}
