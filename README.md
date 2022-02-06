# Apple Disassembler

This project is primarily created to supply a reusable Java disassembler for the AppleCommander project.

## CLI

```
$ acdasm --help        
Usage: acdasm [-hV] [-a=<startAddress>] [--6502 | --6502X | --65C02] <file>

AC Disassembler.
      <file>      File to disassemble.

Options:
  -a, --addr, --origin=<startAddress>

  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

CPU
      --6502      MOS 6502.
      --6502X     MOS 6502 + 'illegal' instructions.
      --65C02     WDC 65C02.
```

Sample run:

```
$ acdasm --6502 -a 0x220 COPY.OBJ1.bin
0220- 88        DEY
0221- B0 02     BCS $0225
0223- A0 B7     LDY #$B7
0225- A9 89     LDA #$89
0227- 20 80 02  JSR $0280
...
```