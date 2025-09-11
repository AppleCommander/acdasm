/*
 * Copyright (C) 2025  rob at applecommander.org
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.applecommander.disassembler.cli;

import picocli.CommandLine;
import picocli.CommandLine.IExecutionExceptionHandler;
import picocli.CommandLine.ParseResult;

// Note: Taken from https://picocli.info/#_business_logic_exceptions
class PrintExceptionMessageHandler implements IExecutionExceptionHandler {
    public int handleExecutionException(Exception ex,
                                        CommandLine cmd,
                                        ParseResult parseResult) {

        // bold red error message
        cmd.getErr().println(cmd.getColorScheme().errorText(ex.getMessage()));

        return cmd.getExitCodeExceptionMapper() != null
                    ? cmd.getExitCodeExceptionMapper().getExitCode(ex)
                    : cmd.getCommandSpec().exitCodeOnExecutionException();
    }
}


