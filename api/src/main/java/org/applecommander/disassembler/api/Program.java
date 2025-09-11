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
    public byte peek(int n) {
        return offset+n < code.length ? code[offset+n] : 0;
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
