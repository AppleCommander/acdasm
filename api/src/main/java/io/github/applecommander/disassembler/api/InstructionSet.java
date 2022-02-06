package io.github.applecommander.disassembler.api;

public interface InstructionSet {
    Instruction decode(Program program);
}
