# Apple Disassembler

[![Current Release](https://img.shields.io/github/release/AppleCommander/acdasm.svg)](https://github.com/AppleCommander/acdasm/releases)
![License](https://img.shields.io/github/license/AppleCommander/acdasm) 

This project is primarily created to supply a reusable Java disassembler for the AppleCommander project.

## API

The disassembler can be used in any Java application. Please review the [API](docs/API.md) section for details.

## CLI

```
$ acdasm --help
Usage: acdasm [-hV] [--debug] [--[no-]descriptions] [--[no-]labels]
              [-a=<startAddress>] [-n=<length>] [--offset=<offset>]
              [-l=<library>[,<library>...]]... [--codefile | --6502s | --65c02
              | --6502x | --sweet16 | --pcode | --6502 | --z80] <file>

AppleCommander Disassembler.

      <file>                 File to disassemble.

Options:
  -a, --addr, --origin=<startAddress>
                             Set start address for application.
      --debug                Print stack traces
      --[no-]descriptions    Include opcode descriptions.
  -h, --help                 Show this help message and exit.
  -l, --library=<library>[,<library>...]
                             Select which library labels to load. Each CPU has
                               a default set. Use 'All' to select all. 'None'
                               may also be used to turn library labels off.
      --[no-]labels          Show or hide labels.
  -n, --length=<length>      Disassembly length bytes.
      --offset=<offset>      Skip offset bytes into binary before disassembling.
  -V, --version              Print version information and exit.

CPU Selection:
      --6502                 MOS 6502.
      --6502s, --6502S       MOS 6502 with SWEET16 switching.
      --6502x, --6502X       MOS 6502 + 'illegal' instructions.
      --65c02, --65C02       WDC 65C02.
      --codefile, --CODEFILE Apple Pascal CODEFILE
      --pcode, --PCODE       Apple Pascal p-code
      --sweet16, --SWEET16   SWEET16.
      --z80, --Z80           Zilog Z80.

Processor Defaults:
  Default Value          6502   6502X  6502S  65C02  SWEET-16  Z80    P-CODE
  ---------------------  -----  -----  -----  -----  --------  -----  ------
  Start Address          $0300  $0300  $0300  $0300  $0300     0100H  $0000
  Library Labels         All    All    All    All    All       None   None
  Max Bytes/Instruction  3      3      3      3      3         5      8
  Show Descriptions?     No     No     No     No     No        No     Yes

Library Groups:
  F800, Applesoft, ProDOS, DOS, DISKII, Softswitches
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
0103- 39                         ADD   HL,SP                          
0104- 22 15 02                   LD    (L0215),HL                     
0107- 31 57 02                   LD    SP,0257H                       
010A- CD C1 01                   CALL  L01C1                          
010D- FE FF                      CP    A,FFH                          
010F- C2 1B 01                   JP    NZ,L011B                       
0112- 11 F3 01                   LD    DE,01F3H                       
0115- CD 9C 01                   CALL  L019C                          
0118- C3 51 01                   JP    L0151 
...
```

The 6502/SWEET16 switching mode will switch to SWEET16 mode if a `JSR $F689` occurs in code and switch back to 6502 mode when a SWEET16 `RTN` instruction is encountered:

```
$ acdasm --6502s renumber.bin
0300- 20 89 F6             JSR   $F689                          
0303-                      .SWEET16                                
0303- 11 CA 00             SET   R1,#$00CA                      
0306- 61                   LDD   @R1                            
0307- 31                   ST    R1                             
0308- 12 0A 00             SET   R2,#$000A                      
030B- 13 4C 00             SET   R3,#$004C                      
030E- 63                   LDD   @R3                            
030F- 33                   ST    R3                             
0310- 14 DE 03             SET   R4,#$03DE                      
0313- 21        L0313      LD    R1                             
0314- D3                   CPR   R3                             
0315- 03 0E                BC    L0325                          
0317- 41                   LD    @R1                            
0318- 35                   ST    R5                             
0319- 24                   LD    R4                             
031A- A2                   ADD   R2                             
031B- 34                   ST    R4                             
031C- 71                   STD   @R1                            
031D- F1                   DCR   R1                             
031E- F1                   DCR   R1                             
031F- F1                   DCR   R1                             
0320- 21                   LD    R1                             
0321- A5                   ADD   R5                             
0322- 31                   ST    R1                             
0323- 01 EE                BR    L0313                          
0325- 00        L0325      RTN                                  
0326-                      .6502                                
0326- 60                   RTS             
```

Labels can also be toggled off:

```
$ acdasm --sweet16 sw16.sample2.bin 
0300- 11 00 0A             SET   R1,#$0A00                      
0303- 12 80 0A             SET   R2,#$0A80                      
0306- 13 23 00             SET   R3,#$0023                      
0309- 41        L0309      LD    @R1                            
030A- 52                   ST    @R2                            
030B- F3                   DCR   R3                             
030C- 07 FB                BNZ   L0309                          
030E- 00                   RTN   
```

Finally, Apple Pascal can be dumped as well. Note that "p-code" expects just p-code up to and including the jump table. The codefile will be more useful:

```
$ acdasm --codefile HELLOWORLD.CODE.bin 
>> Seg #01: FROM=$0200, TO=$0232, N='HELLOWOR', LINKED    , T=$0000, M=P_CODE_LSB, Ver=6
>  Proc#1, Lex Lvl 0, Enter $0200, Exit $0222, Param 4, Data 0, JTAB=$0224
0200- D7                                  NOP                                  ; No operation
0201- D7                                  NOP                                  ; No operation
0202- B6 01 03                            LOD   1,3                            ; Load intermediate word
0205- A6 0C 48 45 4C 4C 4F 20             LSA   'HELLO WORLD!'                 ; Load constant string address
020D- 57 4F 52 4C 44 21 
0213- D7                                  NOP                                  ; No operation
0214- 00                                  SLDC  0                              ; Short load one-word constant
0215- CD 00 13                            CXP   0,19                           ; Call external procedure
0218- 9E 00                               IOC                                  ; IO Check (checks IORESULT)
021A- B6 01 03                            LOD   1,3                            ; Load intermediate word
021D- CD 00 16                            CXP   0,22                           ; Call external procedure
0220- 9E 00                               IOC                                  ; IO Check (checks IORESULT)
0222- C1 00                               RBP   0                              ; Return from base procedure
```

... and the same invoking just p-code:

```
$ acdasm --pcode HELLOWORLD.CODE.bin --offset 0x200 --length 0x24
0200- D7                                  NOP                                  ; No operation
0201- D7                                  NOP                                  ; No operation
0202- B6 01 03                            LOD   1,3                            ; Load intermediate word
0205- A6 0C 48 45 4C 4C 4F 20             LSA   'HELLO WORLD!'                 ; Load constant string address
020D- 57 4F 52 4C 44 21 
0213- D7                                  NOP                                  ; No operation
0214- 00                                  SLDC  0                              ; Short load one-word constant
0215- CD 00 13                            CXP   0,19                           ; Call external procedure
0218- 9E 00                               IOC                                  ; IO Check (checks IORESULT)
021A- B6 01 03                            LOD   1,3                            ; Load intermediate word
021D- CD 00 16                            CXP   0,22                           ; Call external procedure
0220- 9E 00                               IOC                                  ; IO Check (checks IORESULT)
0222- C1 00                               RBP   0                              ; Return from base procedure
```
