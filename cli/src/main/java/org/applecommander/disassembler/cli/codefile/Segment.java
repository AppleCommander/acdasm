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
import java.nio.ByteOrder;

public record Segment(String name, String textInterface, ByteBuffer data, Kind kind, int textAddr,
                      int segNum, MachineType machineType, int version, Object[] dictionary) {

    public static Segment load(String name, int kind, int segInfo, ByteBuffer data, ByteBuffer textAddrBuf) {
        data.order(ByteOrder.LITTLE_ENDIAN);
        var segKind = Kind.values()[kind];
        var textAddr = textAddrBuf.limit();
        var segNum = segInfo & 0x00ff;
        var machineType = MachineType.values()[(segInfo >> 8) & 0xf];
        var version = (segInfo >> 13) & 0x07;

        var pos = data.limit();
        int numProc = data.get(--pos);
        int segNo = data.get(--pos);
        Object[] dictionary = new Object[numProc];

        var textInterface = PascalSupport.textFile(textAddrBuf);

        for (var i=0; i<numProc; i++) {
            pos -= 2;
            var offset = data.getShort(pos);
            var attrs = pos - offset + 1;
            // Note that SYSTEM.PASCAL procedure #30 has offset of $4D76 from position $E16 (table begins at $E56)
            // ... which results to an invalid reference. Unable to find anything in manuals yet.
            if (attrs > 0) {
                dictionary[i] = switch (machineType) {
                    case P_CODE_LSB -> PCodeProcedure.load(data, attrs);
                    case MOS6502 -> {
                        // Not everything in an assembly segment is actually assembly
                        if (data.get(attrs-1) > 0) {
                            yield PCodeProcedure.load(data, attrs);
                        }
                        else {
                            yield AssemblyProcedure.load(data, attrs);
                        }
                    }
                    default -> {
                        System.err.printf("*** WARNING: Unknown machine type: %s - assuming p-code\n", machineType);
                        yield PCodeProcedure.load(data, attrs);
                    }
                };
            }
        }
        return new Segment(name, textInterface, data, segKind, textAddr, segNum, machineType, version, dictionary);
    }

    public enum Kind {
        LINKED, HOSTSEG, SEGPROC, UNITSEG, SEPRTSEG, UNLINKED_INSTRINS, LINKED_INTRINS, DATASEG
    }

    public enum MachineType {
        UNIDENTIFIED, P_CODE_MSB, P_CODE_LSB, PDP11, I8080, Z80, FA440, MOS6502, M6800, TI9900
    }
}
