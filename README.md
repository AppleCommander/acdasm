# Apple Disassembler

[![Current Release](https://img.shields.io/github/release/AppleCommander/acdasm.svg)](https://github.com/AppleCommander/acdasm/releases)
![License](https://img.shields.io/github/license/AppleCommander/acdasm) 

This project is primarily created to supply a reusable Java disassembler for the AppleCommander project.

## CLI

```
$ acdasm --help
Usage: acdasm [-hV] [--debug] [--[no-]labels] [-a=<startAddress>]
              [--offset=<offset>] [--library=<library>[,<library>...]]...
              [--6502S | --Z80 | --6502 | --65C02 | --6502X | --SWEET16] <file>

AC Disassembler.
      <file>              File to disassemble.

Options:
  -a, --addr, --origin=<startAddress>
                          Set start address for application.
      --debug             Print stack traces
  -h, --help              Show this help message and exit.
      --[no-]labels       Show or hide labels.
      --library=<library>[,<library>...]
                          Select which library labels to load. Use 'All' to
                            select all. Each CPU has a default set (most are
                            'All' except Z80).  Options are: 'F800',
                            'Applesoft', 'ProDOS', 'DOS', 'DISKII'. 'None' may
                            also be used to turn library labels off.
      --offset=<offset>   Skip offset bytes into binary before disassembling.
  -V, --version           Print version information and exit.

CPU Selection:
      --6502              MOS 6502.
      --6502S             MOS 6502 with SWEET16 switching.
      --6502X             MOS 6502 + 'illegal' instructions.
      --65C02             WDC 65C02.
      --SWEET16           SWEET16.
      --Z80               Zilog Z80.
```

Sample runs:

```
$ acdasm --6502 --addr 0x2a0 COPY.OBJ0.bin 
02A0- AD D8 03             LDA   $03D8
02A3- 85 3D                STA   A1H
02A5- A9 68                LDA   #$68
02A7- 85 3C                STA   A1L
02A9- AD D0 02             LDA   L02D0
02AC- A0 00                LDY   #$00
02AE- 91 3C                STA   (A1L),Y
02B0- C8                   INY   
02B1- C8                   INY   
02B2- AD CE 02             LDA   L02CE
...
```

```
$ acdasm --Z80 DUMP.COM.bin 
0100- 21 00 00                   LD    HL,0000H
0103- 39                         ADD   SP
0104- 22 15 02                   LD    L0215,HL
0107- 31 57 02                   LD    SP,0257H
010A- CD C1 01                   CALL  L01C1
010D- FE FF                      CP    FFH
010F- C2 1B 01                   JP    NZ,L011B
0112- 11 F3 01                   LD    DE,01F3H
0115- CD 9C 01                   CALL  L019C
0118- C3 51 01                   JP    L0151
...
```

The 6502/SWEET16 switching mode will switch to SWEET16 mode if a `JSR $F689` occurs in code and switch back to 6502 mode when a SWEET16 `RTN` instruction is encountered:

```
$ acdasm -a 0x800 --6502S renumber.bin
0800- 20 89 F6             JSR $F689
0803-                      .SWEET16
0803- 11 CA 00             SET R1,#$00CA
0806- 61                   LDD @R1
0807- 31                   ST R1
0808- 12 0A 00             SET R2,#$000A
080B- 13 4C 00             SET R3,#$004C
080E- 63                   LDD @R3
080F- 33                   ST R3
0810- 14 DE 03             SET R4,#$03DE
0813- 21        L0813      LD R1
0814- D3                   CPR R3
0815- 03 0E     L0825      BC $0825
0817- 41                   LD @R1
0818- 35                   ST R5
0819- 24                   LD R4
081A- A2                   ADD R2
081B- 34                   ST R4
081C- 71                   STD @R1
081D- F1                   DCR R1
081E- F1                   DCR R1
081F- F1                   DCR R1
0820- 21                   LD R1
0821- A5                   ADD R5
0822- 31                   ST R1
0823- 01 EE     L0813      BR $0813
0825- 00        L0825      RTN
0826-                      .6502
0826- 60                   RTS
```

Labels can also be toggled off:

```
$ acdasm --SWEET16 --no-labels sw16.sample2.bin
0300- 11 00 0A  SET R1,#$0A00
0303- 12 80 0A  SET R2,#$0A80
0306- 13 23 00  SET R3,#$0023
0309- 41        LD @R1
030A- 52        ST @R2
030B- F3        DCR R3
030C- 07 FB     BNZ $0309
030E- 00        RTN
```
