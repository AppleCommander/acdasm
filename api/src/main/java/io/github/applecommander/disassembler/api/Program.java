package io.github.applecommander.disassembler.api;

import java.util.Arrays;
import java.util.Objects;

public class Program {
    static final int ADDRESS_SPACE = 64 * 1024; //64k Address Space

    private int baseAddress;
    private int offset;
    private byte[] code;
    
    public Program(byte[] code, int address) {
        Objects.requireNonNull(code);
        this.baseAddress = address;
        this.code = code;
        this.offset = 0;
    }

    public boolean hasMore() {
        return offset < code.length;
    }
    public byte peek() {
        return hasMore() ? code[offset] : 0;
    }
    public byte[] read(int n) {
        byte[] x = Arrays.copyOfRange(code, offset, offset+n);
        offset += n;
        return x;
    }
    public int currentOffset() {
        return offset;
    }
    public int currentAddress() {
        return (baseAddress+offset) % ADDRESS_SPACE; //wrap around to 0 if address exceeds the address space
    }
}
