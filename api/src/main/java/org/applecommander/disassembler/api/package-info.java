/// Core disassembler API for decoding machine code into human-readable assembly instructions.
///
/// This package provides the main entry point and shared types used across all instruction sets.
/// Use `Disassembler` to decode a byte array into a list of `Instruction` objects.  The
/// `InstructionSet` interface defines the contract for each supported CPU architecture, and
/// `Program` tracks the decoding state and provides helper methods for reading operands.
///
/// Custom function mappings, such as ProDOS MLI calls, are supported via `FunctionMapping`.
///
/// @see org.applecommander.disassembler.api.Disassembler
/// @see org.applecommander.disassembler.api.InstructionSet
/// @see org.applecommander.disassembler.api.Instruction
/// @see org.applecommander.disassembler.api.Program
package org.applecommander.disassembler.api;
