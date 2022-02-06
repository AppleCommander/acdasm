package io.github.applecommander.disassembler.api;

public interface Instruction {
    int getAddress();
    int getLength();
    byte[] getBytes();
    String getOpcodeMnemonic();
    boolean operandHasAddress();
    int getOperandValue();
    String formatOperandWithValue();
    String formatOperandWithLabel(String label);
}
