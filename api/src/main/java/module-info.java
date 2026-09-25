module org.applecommander.disassembler {
    requires ini4j;

    exports org.applecommander.disassembler.api;
    exports org.applecommander.disassembler.api.mos6502;
    exports org.applecommander.disassembler.api.pcode;
    exports org.applecommander.disassembler.api.sweet16;
    exports org.applecommander.disassembler.api.switching6502;
    exports org.applecommander.disassembler.api.z80;
}