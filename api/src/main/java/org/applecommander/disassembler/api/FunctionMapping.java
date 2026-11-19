package org.applecommander.disassembler.api;

import java.util.List;
import java.util.Map;

/// Custom mappings for functions that pull parameters inline of the code.
///
/// The functions are identified by a sequence of bytes (presumably a `JSR` to
/// some address) followed by parameter definitions. The description is done
/// in [TOML](https://toml.io/en/) simply because it was already in use within
/// the project.
///
/// Please note that the _only_ sample identified at this point is the ProDOS
/// MLI. So it is only tested with that. Known weaknesses are: `LOOKUP` only
/// does a single-byte value lookup. `ADDRESS` assumes two bytes and assumes
/// little-endian (lo/hi) address format.
///
/// The ProDOS MLI is an example:
///
/// ```
/// # 20 00 BF   JSR MLI
/// # XX         DFB function
/// # XX XX      DA parameters
///
/// code = [0x20, 0x00, 0xbf]
///
/// [[parameters]]
/// name = "function"
/// size = 1
/// type = "lookup"
/// values."0x40" = "ALLOC_INTERRUPT"
/// <snip>
///
/// [[parameters]]
/// name = "parameters"
/// size = 2
/// type = "address"
/// ```
///
/// @param code The identification bytes; likely a `JSR` sequence of `0x20` and an address.
/// @param parameters Parameter definitions.
public record FunctionMapping(byte[] code, List<Parameter> parameters) {

    /// Parameter type.
    /// `LOOKUP` is (currently) a single-byte lookup value. See the MLI example.
    /// `ADDRESS` is a typical little-endian address.
    public enum ParamType { LOOKUP, ADDRESS }

    /// A parameter definition.
    ///
    /// @param name The name of this parameter. Currently unused but could conceivably be used in a comment.
    /// @param size The number of bytes for this parameter.
    /// @param type The type of parameter. See `ParamType` for information.
    /// @param values Value lookup table for a `LOOKUP` parameter type. The text is shown in the disassembly comments.
    public record Parameter(String name, int size, ParamType type, Map<Integer,String> values) {}
}
