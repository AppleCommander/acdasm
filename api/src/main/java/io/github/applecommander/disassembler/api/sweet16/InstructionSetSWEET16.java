package io.github.applecommander.disassembler.api.sweet16;

import io.github.applecommander.disassembler.api.Instruction;
import io.github.applecommander.disassembler.api.InstructionSet;
import io.github.applecommander.disassembler.api.Program;

public class InstructionSetSWEET16 implements InstructionSet {
    public static InstructionSetSWEET16 forSWEET16() {
        return new InstructionSetSWEET16();
    }

    private InstructionSetSWEET16() {
        // Prevent construction
    }
    
    @Override
    public Instruction decode(Program program) {
        int op = Byte.toUnsignedInt(program.peek());
        int low = op & 0x0f;
        int high = (op & 0xf0) >> 4;
        
        int register = low;
        AddressModeSWEET16 addressMode;
        OpcodeSWEET16 opcode;
        if (high == 0) {
            opcode = OpcodeSWEET16.NON_REGISTER_OPS[low];
            addressMode = AddressModeSWEET16.NON_REGISTER_OPS[low];
        }
        else {
            opcode = OpcodeSWEET16.REGISTER_OPS[high];
            addressMode = AddressModeSWEET16.REGISTER_OPS[high];
        }
        
        if (opcode == OpcodeSWEET16.ZZZ) {
            addressMode = AddressModeSWEET16.IMP;
        }

        int currentAddress = program.currentAddress();  // Need capture before read
        byte[] code = program.read(addressMode.getInstructionLength());

        return new InstructionSWEET16(addressMode, opcode, register, currentAddress, code);
    }
}
