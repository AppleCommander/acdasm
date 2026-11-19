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
package org.applecommander.disassembler.api.switching6502;

import org.applecommander.disassembler.api.Instruction;
import org.applecommander.disassembler.api.Instruction.Operand;
import org.applecommander.disassembler.api.Instruction.Target;
import org.applecommander.disassembler.api.InstructionSet;
import org.applecommander.disassembler.api.Program;
import org.applecommander.disassembler.api.mos6502.InstructionSet6502;
import org.applecommander.disassembler.api.sweet16.InstructionSetSWEET16;

import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.function.Function;

public class InstructionSet6502Switching implements InstructionSet {
    public static InstructionSet withSwitching() {
        return new InstructionSet6502Switching(InstructionSet6502.for6502(), InstructionSetSWEET16.forSWEET16());
    }
    
    private final InstructionSet6502 mos6502;
    private final InstructionSetSWEET16 sweet16;
    private Function<Program,Instruction> strategy = this::decode6502;
    private final Queue<Instruction> pending = new LinkedList<>();
    
    private InstructionSet6502Switching(InstructionSet6502 mos6502, InstructionSetSWEET16 sweet16) {
        this.mos6502 = mos6502;
        this.sweet16 = sweet16;
    }

    @Override
    public Defaults defaults() {
        return Defaults.builder()
                .startAddress(0x300)
                .libraryLabels("All")
                .bytesPerInstruction(3)
                .get();
    }

    @Override
    public Instruction decodeOne(Program program) {
        if (!pending.isEmpty()) {
            return pending.remove();
        }
        return strategy.apply(program);
    }

    @Override
    public List<OpcodeTable> opcodeTables() {
        throw new RuntimeException("Not implemented");
    }

    Instruction decode6502(Program program) {
        Instruction instruction = mos6502.decodeOne(program);
        int operandAddress = instruction.addressRef()
                .flatMap(Operand::target)
                .map(Target::address)
                .orElse(0);
        if ("JSR".equals(instruction.mnemonic()) && operandAddress == 0xf689) {
            strategy = this::decodeSWEET16;
            pending.add(Instruction.at(program.currentAddress()).mnemonic(".SWEET16").get());
        }
        return instruction;
    }
    
    Instruction decodeSWEET16(Program program) {
        Instruction instruction = sweet16.decodeOne(program);
        if ("RTN".equals(instruction.mnemonic())) {
            strategy = this::decode6502;
            pending.add(Instruction.at(program.currentAddress()).mnemonic(".6502").get());
        }
        return instruction;
    }
}
