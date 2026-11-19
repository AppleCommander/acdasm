/*
 * Copyright 2026  Robert Greene
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.applecommander.disassembler.cli.codefile;

import java.nio.ByteBuffer;

public record AssemblyProcedure(ByteBuffer data, int procNum, int relocSegNum, int enterIC, int attrs, int endIC,
                                int[] baseRelativeReloc, int[] segRelativeReloc, int[] procRelativeReloc,
                                int[] interpRelativeReloc) {

    public byte[] codeBytes() {
        byte[] bytes = new byte[endIC - enterIC];
        data.get(enterIC, bytes);
        return bytes;
    }

    public static AssemblyProcedure load(ByteBuffer data, int attrs) {
        int relocSegNum = data.get(attrs);
        int procNum = data.get(attrs-1);
        assert procNum == 0;
        int enterIC = attrs - data.getShort(attrs-3) - 3;
        // SELF RELATIVE POINTERS contains the _distance_ between low-order byte of pointer
        // A little bit stinky because Java doesn't allow multiple returns.
        // which would allow '(attrs,baseRelativeReloc) = selfRelativeTable(attrs)'
        var pair = loadSelfRelativeTable(data, attrs-3);
        int[] baseRelativeReloc = pair.table();
        pair = loadSelfRelativeTable(data, pair.attrs());
        int[] segRelativeReloc = pair.table();
        pair = loadSelfRelativeTable(data, pair.attrs());
        int[] procRelativeReloc = pair.table();
        pair = loadSelfRelativeTable(data, pair.attrs());
        int[] interpRelativeReloc = pair.table();
        int endIC = pair.attrs();
        return new AssemblyProcedure(data, procNum, relocSegNum, enterIC, attrs, endIC,
                baseRelativeReloc, segRelativeReloc, procRelativeReloc, interpRelativeReloc);
    }

    private static Result<Integer,int[]> loadSelfRelativeTable(ByteBuffer data, int base) {
        base -= 2;
        int n = data.getShort(base);
        int[] table = new int[n];
        for (int i=0; i<n; i++) {
            base -= 2;
            table[i] = base - data.getShort(base);
        }
        return new Result<>(base, table);
    }

    record Result<I,J>(I attrs, J table) {}
}
