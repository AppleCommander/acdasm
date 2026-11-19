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

public class PascalSupport {
    private PascalSupport() {
        // prevent construction
    }

    public static String textFile(ByteBuffer textAddrBuf) {
        StringBuilder sb = new StringBuilder();
        while (textAddrBuf.hasRemaining()) {
            var ch = Byte.toUnsignedInt(textAddrBuf.get());
            if (ch == 0) {
                // 0's seem to be the end?
                break;
            }
            else if (ch == 16) {
                // DLE
                var n = Byte.toUnsignedInt(textAddrBuf.get()) - 32;
                while (n-- > 0) sb.append(" ");
            }
            else if (ch == 13) {
                // CR
                sb.append('\n');
            }
            else {
                sb.append((char)(ch&0x7f));
            }
        }
        return sb.toString();
    }
}
