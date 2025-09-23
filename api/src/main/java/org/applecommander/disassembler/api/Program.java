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

import java.util.Arrays;
import java.util.Objects;

public class Program {
    static final int ADDRESS_SPACE = 64 * 1024; //64k Address Space

    private final int baseAddress;
    private final byte[] code;
    private int offset;
    // Informational point in code. Serves as a context for InstructionSet.
    private int mark;

    public Program(byte[] code, int address) {
        Objects.requireNonNull(code);
        this.baseAddress = address;
        this.code = code;
        this.offset = 0;
        this.mark = 0;
    }

    public int length() {
        return code.length;
    }
    public boolean hasMore() {
        return offset < code.length;
    }
    public int peekUnsignedByte() {
        return peekUnsignedByte(0);
    }
    public int peekUnsignedByte(int n) {
        return offset+n < code.length ? Byte.toUnsignedInt(code[offset+n]) : 0;
    }
    public int peekSignedByte(int n) {
        return offset+n < code.length ? code[offset+n] : 0;
    }
    public byte[] read(int n) {
        byte[] x = Arrays.copyOfRange(code, offset, offset+n);
        offset += n;
        return x;
    }
    /** Get an unsigned byte from specified offset. This is not relative like the others. */
    public int getUnsignedByte(int n) {
        return n < code.length ? Byte.toUnsignedInt(code[n]) : 0;
    }
    public int currentOffset() {
        return offset;
    }
    public int baseAddress() {
        return baseAddress;
    }
    public int currentAddress() {
        return (baseAddress+offset) % ADDRESS_SPACE; //wrap around to 0 if address exceeds the address space
    }
    public int mark() {
        return mark;
    }
    public void mark(int mark) {
        this.mark = mark;
    }
}
