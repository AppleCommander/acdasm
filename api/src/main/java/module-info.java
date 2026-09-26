module org.applecommander.disassembler {
    requires com.fasterxml.jackson.dataformat.toml;
    requires com.fasterxml.jackson.core;

    exports org.applecommander.disassembler.api;
    exports org.applecommander.disassembler.api.mos6502;
    exports org.applecommander.disassembler.api.pcode;
    exports org.applecommander.disassembler.api.sweet16;
    exports org.applecommander.disassembler.api.switching6502;
    exports org.applecommander.disassembler.api.z80;
}