# Graal Native Image configuration

This is a mish-mash of manual and automatic code generation.

To _update_ the configurations, use:

```declarative
-agentlib:native-image-agent=config-merge-dir=cli/src/main/resources/META-INF/native-image
```

Please delete empty files and reformat the JSON!

When running the JAR from the command line. This particular pathing, suggsets that it be run from the root of the project.

For example:

```shell
$ java -agentlib:native-image-agent=config-merge-dir=cli/src/main/resources/META-INF/native-image \
       -jar cli/build/libs/acdasm.jar \
       ~/Documents/Source/izapple2/resources/Apple\ Disk\ II\ 13\ Sector\ Interface\ Card\ ROM\ P5\ -\ 341-0009.bin \
       -a 0xc600
C600- A2 20                LDX #$20
C602- A0 00                LDY #$00
C604- A9 03     LC604      LDA #$03
C606- 85 3C                STA $3C
C608- 18                   CLC
C609- 88                   DEY
<snip>
```