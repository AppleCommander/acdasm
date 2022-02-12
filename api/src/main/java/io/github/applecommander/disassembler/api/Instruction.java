package io.github.applecommander.disassembler.api;

import java.util.Optional;

public interface Instruction {
    int getAddress();
    int getLength();
    byte[] getBytes();
    Optional<String> getAddressLabel();
    void setAddressLabel(String label);
    String getOpcodeMnemonic();
    boolean operandHasAddress();
    int getOperandValue();
    void setOperandLabel(String label);
    String formatOperandWithValue();
    String formatOperandWithLabel();
}
