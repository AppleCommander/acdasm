# Apple Disassembler

This project is primarily created to supply a reusable Java disassembler for the AppleCommander project.

## CLI

```
$ acdasm --help
Usage: acdasm [-hV] [-a=<startAddress>] [--6502S | --SWEET16 | --6502X |
              --65C02 | --6502] <file>

AC Disassembler.
      <file>      File to disassemble.

Options:
  -a, --addr, --origin=<startAddress>

  -h, --help      Show this help message and exit.
  -V, --version   Print version information and exit.

CPU
      --6502      MOS 6502.
      --6502S     MOS 6502 with SWEET16 switching.
      --6502X     MOS 6502 + 'illegal' instructions.
      --65C02     WDC 65C02.
      --SWEET16   SWEET16.
```

Sample runs:

```
$ acdasm --6502 -a 0x220 COPY.OBJ1.bin
0220- 88        DEY
0221- B0 02     BCS $0225
0223- A0 B7     LDY #$B7
0225- A9 89     LDA #$89
0227- 20 80 02  JSR $0280
...
```

The 6502/SWEET16 switching mode will switch to SWEET16 mode if a `JSR $F689` occurs in code and switch back to 6502 mode when a SWEET16 `RTN` instruction is encountered:

```
$ acdasm -a 0x800 --6502S renumber.bin
0800- 20 89 F6  JSR $F689
0803-           .SWEET16
0803- 11 CA 00  SET R1,#$00CA
0806- 61        LDD @R1
0807- 31        ST R1
0808- 12 0A 00  SET R2,#$000A
080B- 13 4C 00  SET R3,#$004C
080E- 63        LDD @R3
080F- 33        ST R3
0810- 14 DE 03  SET R4,#$03DE
0813- 21        LD R1
0814- D3        CPR R3
0815- 03 0E     BC $0825
0817- 41        LD @R1
0818- 35        ST R5
0819- 24        LD R4
081A- A2        ADD R2
081B- 34        ST R4
081C- 71        STD @R1
081D- F1        DCR R1
081E- F1        DCR R1
081F- F1        DCR R1
0820- 21        LD R1
0821- A5        ADD R5
0822- 31        ST R1
0823- 01 EE     BR $0813
0825- 00        RTN
0826-           .6502
0826- 60        RTS
```
