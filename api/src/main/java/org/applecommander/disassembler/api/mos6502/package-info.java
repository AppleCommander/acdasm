/// MOS Technology 6502 and WDC 65C02 instruction set support.
///
/// This package provides decoding for the classic 6502 family of CPUs, including three variants:
/// the original MOS 6502, the 6502 with undocumented (illegal) opcodes, and the WDC 65C02.
///
/// Use the following to obtain the desired instruction set:
/// * `InstructionSet6502.for6502()`
/// * `InstructionSet6502.for6502withIllegalInstructions()`
/// * `InstructionSet6502.for65C02()`
///
/// @see org.applecommander.disassembler.api.mos6502.InstructionSet6502
/// @see org.applecommander.disassembler.api.mos6502.Opcode6502
/// @see org.applecommander.disassembler.api.mos6502.AddressMode6502
package org.applecommander.disassembler.api.mos6502;
