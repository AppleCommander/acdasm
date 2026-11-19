/// Hybrid 6502/SWEET16 switching instruction set support.
///
/// This package provides an instruction set that automatically switches between
/// 6502 and SWEET16 decoding modes.  When a `JSR` to address `$F689` is encountered,
/// the decoder switches to SWEET16 mode; when an SWEET16 `RTN` instruction is
/// decoded, it switches back to 6502 mode.  Use `InstructionSet6502Switching.withSwitching()`
/// to obtain the hybrid instruction-set instance.
///
/// @see org.applecommander.disassembler.api.switching6502.InstructionSet6502Switching
package org.applecommander.disassembler.api.switching6502;
