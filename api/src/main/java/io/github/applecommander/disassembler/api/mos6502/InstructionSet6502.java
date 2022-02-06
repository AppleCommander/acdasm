package io.github.applecommander.disassembler.api.mos6502;

import io.github.applecommander.disassembler.api.Instruction;
import io.github.applecommander.disassembler.api.InstructionSet;
import io.github.applecommander.disassembler.api.Program;

public class InstructionSet6502 implements InstructionSet {
    public static InstructionSet6502 for6502() {
        return new InstructionSet6502(AddressMode6502.MOS6502, Opcode6502.MOS6502);
    }
    public static InstructionSet6502 for6502withIllegalInstructions() {
        return new InstructionSet6502(AddressMode6502.MOS6502, Opcode6502.MOS6502_WITH_ILLEGAL);
    }
    public static InstructionSet6502 for65C02() {
        return new InstructionSet6502(AddressMode6502.WDC65C02, Opcode6502.WDC65C02);
    }
    
    private AddressMode6502[] addressModes;
    private Opcode6502[] opcodes;
    
    private InstructionSet6502(AddressMode6502[] addressModes, Opcode6502[] opcodes) {
        this.addressModes = addressModes;
        this.opcodes = opcodes;
    }

    @Override
    public Instruction decode(Program program) {
        int op = Byte.toUnsignedInt(program.peek());
        
        AddressMode6502 addressMode = addressModes[op];
        Opcode6502 opcode = opcodes[op];

        if (isInvalidInstruction(opcode, op)) {
            addressMode = AddressMode6502.IMP;
        }
        
        int currentAddress = program.currentAddress();  // Need capture before read
        byte[] code = program.read(addressMode.getInstructionLength());
        
        return new Instruction6502(addressMode, opcode, currentAddress, code);
    }

    public boolean isInvalidInstruction(Opcode6502 opcode, int op) {
        if (opcode == Opcode6502.ZZZ) {
            return true;
        }
        else if (opcode == Opcode6502.NOP && op != 0xea) {
            return true;
        }
        return false;
    }
}
