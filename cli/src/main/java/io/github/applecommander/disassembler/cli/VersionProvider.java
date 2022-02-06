package io.github.applecommander.disassembler.cli;

import picocli.CommandLine.IVersionProvider;

/** Display version information.  Note that this is dependent on the Spring Boot Gradle plugin configuration. */
public class VersionProvider implements IVersionProvider {
    public String[] getVersion() {
    	return new String[] { 
            String.format("acdasm: %s", Main.class.getPackage().getImplementationVersion()),
		};
    }
}