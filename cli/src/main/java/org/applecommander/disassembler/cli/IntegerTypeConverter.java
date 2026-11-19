/*
 * Copyright 2026  Robert Greene
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.applecommander.disassembler.cli;

import picocli.CommandLine.ITypeConverter;

/** Add support for "$801" and "0x801" instead of just decimal like 2049. */
public class IntegerTypeConverter implements ITypeConverter<Integer> {
	@Override
	public Integer convert(String value) {
		if (value == null) {
			return null;
		} else if (value.startsWith("$")) {
			return Integer.valueOf(value.substring(1), 16);
		} else if (value.startsWith("0x") || value.startsWith("0X")) {
            return Integer.valueOf(value.substring(2), 16);
        } else if (value.endsWith("H") || value.endsWith("h")) {
            return Integer.valueOf(value.substring(0, value.length()-1), 16);
		} else {
			return Integer.valueOf(value);
		}
	}
}