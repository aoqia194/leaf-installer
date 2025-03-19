/*
 * Copyright (c) 2016-2025 FabricMC, aoqia
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
package dev.aoqia.leaf.installer.util;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class ArgumentParser {
    private final String[] args;
    private Map<String, String> argMap;
    //The command will be the first argument passed, and if it doesnt start with -
    private String command = null;

    private ArgumentParser(String[] args) {
        this.args = args;
        parse();
    }

    public static ArgumentParser create(String[] args) {
        return new ArgumentParser(args);
    }

    public String get(String argument) {
        if (!argMap.containsKey(argument)) {
            throw new IllegalArgumentException(String.format("Could not find %s in the arguments", argument));
        }

        String arg = argMap.get(argument);

        if (arg == null) {
            throw new IllegalArgumentException(String.format("Could not value for %s", argument));
        }

        return arg;
    }

    public String getOrDefault(String argument, Supplier<String> stringSuppler) {
        if (!argMap.containsKey(argument)) {
            return stringSuppler.get();
        }

        return argMap.get(argument);
    }

    public boolean has(String argument) {
        return argMap.containsKey(argument);
    }

    public void ifPresent(String argument, Consumer<String> consumer) {
        if (has(argument)) {
            consumer.accept(get(argument));
        }
    }

    public Optional<String> getCommand() {
        return command == null ? Optional.empty() : Optional.of(command);
    }

    private void parse() {
        argMap = new HashMap<>();

        for (int i = 0; i < args.length; i++) {
            if (args[i].startsWith("-")) {
                String key = args[i].substring(1);
                String value = null;

                // If it's an arg like -test=123
                if (args[i].contains("=")) {
                    final String truekey = key.substring(0, key.indexOf("="));
                    value = key.substring(key.indexOf("=") + 1);
                    argMap.put(truekey, value);
                    continue;
                }

                if (argMap.containsKey(key)) {
                    throw new IllegalArgumentException(String.format("Argument %s already passed", key));
                }

                argMap.put(key, value);
            } else if (i == 0) {
                command = args[i];
            }
        }
    }
}
