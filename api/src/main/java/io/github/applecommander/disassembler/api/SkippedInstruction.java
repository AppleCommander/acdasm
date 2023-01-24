package io.github.applecommander.disassembler.api;

import java.util.Optional;

public class SkippedInstruction implements Instruction {
    public static Instruction from(Program program) {
        int currentAddress = program.currentAddress();  // Need capture before read
        byte[] code = program.read(1);
        return new SkippedInstruction(currentAddress, code);
    }
    
    private int address;
    private byte[] code;
    private String addressLabel;
    
    SkippedInstruction(int address, byte[] code) {
        this.address = address;
        this.code = code;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public int getLength() {
        return 1;
    }

    @Override
    public byte[] getBytes() {
        return code;
    }
    
    @Override
    public Optional<String> getAddressLabel() {
        return Optional.ofNullable(addressLabel);
    }
    
    @Override
    public void setAddressLabel(String label) {
        this.addressLabel = label;
    }
    
    @Override
    public String getOpcodeMnemonic() {
        return "---";
    }

    @Override
    public boolean operandHasAddress() {
        return false;
    }

    @Override
    public int getOperandValue() {
        return 0;
    }
    
    @Override
    public void setOperandLabel(String label) {
        // Unused, ignoring.
    }

    @Override
    public String formatOperandWithValue() {
        return getOpcodeMnemonic();
    }

    @Override
    public String formatOperandWithLabel() {
        return getOpcodeMnemonic();
    }
}