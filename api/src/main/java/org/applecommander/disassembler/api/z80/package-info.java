/// Z80 instruction set support.
///
/// This package provides decoding for the Zilog Z80 CPU, including the main opcode
/// set as well as the extended 'ED' and 'CB' prefix tables.  It supports IX/IY
/// index registers, conditional branches, and I/O port operations.  Use
/// `InstructionSetZ80.forZ80()` to obtain the Z80 instruction-set instance.
///
/// @see org.applecommander.disassembler.api.z80.InstructionSetZ80
package org.applecommander.disassembler.api.z80;
