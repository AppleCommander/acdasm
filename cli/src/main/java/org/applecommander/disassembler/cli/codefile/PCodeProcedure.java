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

public record PCodeProcedure(int procNum, int lexLevel, int enterIC, int exitIC, int paramsSize,
                             int dataSize, int jumpTable, ByteBuffer data) {

    public byte[] codeBytes() {
        byte[] bytes = new byte[jumpTable()-enterIC()];
        data.get(enterIC(), bytes);
        return bytes;
    }
    public static PCodeProcedure load(ByteBuffer data, int attrs) {
        int lexLevel = data.get(attrs);
        int procNum = data.get(attrs-1);
        // SELF RELATIVE POINTERS contains the _distance_ between low-order byte of pointer
        int enterIC = attrs - data.getShort(attrs-3) - 3;
        int exitIC = attrs - data.getShort(attrs-5) - 5;
        int paramsSize = data.getShort(attrs-7);
        int dataSize = data.getShort(attrs-9);
        int jumpTable = attrs - 9;
        return new PCodeProcedure(procNum, lexLevel, enterIC, exitIC, paramsSize, dataSize, jumpTable, data);
    }
}
