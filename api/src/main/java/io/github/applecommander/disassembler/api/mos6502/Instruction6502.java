package io.github.applecommander.disassembler.api.mos6502;

import java.util.Optional;

import io.github.applecommander.disassembler.api.Instruction;

public class Instruction6502 implements Instruction {
    private AddressMode6502 addressMode;
    private Opcode6502 opcode;
    private int address;
    private byte[] code;
    private String addressLabel;
    private String operandLabel;
    
    Instruction6502(AddressMode6502 addressMode, Opcode6502 opcode, int address, byte[] code) {
        this.addressMode = addressMode;
        this.opcode = opcode;
        this.address = address;
        this.code = code;
    }

    @Override
    public int getAddress() {
        return address;
    }

    @Override
    public int getLength() {
        return addressMode.getInstructionLength();
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
        return opcode.getMnemonic();
    }

    @Override
    public boolean operandHasAddress() {
        return addressMode.isOperandAbsoluteAddress() || addressMode.isOperandRelativeAddress();
    }

    @Override
    public int getOperandValue() {
        switch (getLength()) {
        case 3:
            return Byte.toUnsignedInt(code[1]) + Byte.toUnsignedInt(code[2])*256;
        case 2:
            if (addressMode.isOperandRelativeAddress()) {
                return address + 2 + code[1];   // allow sign extension
            }
            else {
                return Byte.toUnsignedInt(code[1]);
            }
        default:
            return 0;
        }
    }
    
    @Override
    public void setOperandLabel(String label) {
        this.operandLabel = label;
    }

    @Override
    public String formatOperandWithValue() {
        String label = "A";
        if (addressMode.isOperandAbsoluteAddress() || addressMode.isOperandRelativeAddress()|| getLength() == 3) {
            label = String.format("$%04X", getOperandValue());
        }
        else if (getLength() == 2) {
            label = String.format("$%02X",getOperandValue());
        }
        return internalFormat(label);
    }

    @Override
    public String formatOperandWithLabel() {
        if (operandLabel == null) {
            return formatOperandWithValue();
        }
        return internalFormat(operandLabel);
    }
    
    String internalFormat(String value) {
        if (getLength() == 1) {
            return String.format(addressMode.getInstructionFormat(), getOpcodeMnemonic());
        }
        return String.format(addressMode.getInstructionFormat(), getOpcodeMnemonic(), value);
    }
}