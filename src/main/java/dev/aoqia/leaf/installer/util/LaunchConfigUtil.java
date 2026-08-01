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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import dev.aoqia.leaf.installer.Main;
import dev.aoqia.leaf.installer.util.json.LauncherConfig;

public class LaunchConfigUtil {
    private static final String ORIGINAL_CONFIG_NAME = "ProjectZomboid64.json";

    private final Path gameDir;
    private final Path libsDir;

    public LaunchConfigUtil(Path gameDir) {
        this.gameDir = gameDir;
        this.libsDir = gameDir.resolve(Utils.LEAF_FOLDER).resolve("libraries");
    }

    private static Path getLaunchScriptPath(Path gameDir) {
        return switch (OperatingSystem.CURRENT) {
            case WINDOWS -> gameDir.resolve("StartServer64.bat");
            case LINUX -> gameDir.resolve("start-server.sh");
            case MACOS -> throw new RuntimeException("MacOS launch scripts not implemented.");
        };
    }

    public void createConfig(String name, String mainClass) throws IOException {
        final var originalConfig = this.gameDir.resolve(ORIGINAL_CONFIG_NAME);
        final var newConfig = this.gameDir.resolve(name + ".json");

        if (Files.notExists(originalConfig)) {
            throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.config"));
        }

        try {
            Files.copy(originalConfig, newConfig, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy bootstrapper config: ", e);
        }

        LauncherConfig newConfigJson;
        try {
            newConfigJson = Utils.deserializeJson(Files.readString(newConfig), LauncherConfig.class);
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bootstrapper config: ", e);
        }

        newConfigJson.setMainClass(mainClass);

        // Add our loader's libraries to the classpath.
        // Java 6+ supports cp wildcards but the bootstrapper hard crashes with them.
        final List<String> classpath = newConfigJson.getClasspath();
        try (Stream<Path> stream = Files.walk(libsDir).filter(Files::isRegularFile)) {
            stream.forEach(path -> classpath.add(this.gameDir.relativize(path).toString()));
        }

        try {
            Files.writeString(newConfig, newConfigJson.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write bootstrapper config: ", e);
        }
    }

    public void createScript(String name, String mainClass) throws IOException {
        final var template = getLaunchScriptPath(this.gameDir);
        if (Files.notExists(template)) {
            throw new RuntimeException("Server launch script doesn't exist.");
        }

        final var templateData = Files.readString(template);
        if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
            createScriptWindows(name, mainClass, templateData);
            // } else if (OperatingSystem.CURRENT == OperatingSystem.LINUX) {
            //     createScriptLinux(name, mainClass, templateData);
        } else {
            throw new RuntimeException(
                "Server launch script support not implemented for this OS. Please raise an issue "
                    + "on leaf-installer's GitHub repository!");
        }
    }

    private void createScriptWindows(String name, String mainClass, String templateData) throws IOException {
        final var pattern = Pattern.compile("^SET\\s*PZ_CLASSPATH=(.+)[\\r\\n]+.*-cp.*(zombie(?:\\.\\w+)+)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

        final var matcher = pattern.matcher(templateData);
        if (!matcher.find()) {
            throw new RuntimeException("Failed to find match regex in launch script");
        }

        var libs = new StringBuilder(matcher.group(1));
        try (final var stream = Files.walk(this.libsDir).filter(Files::isRegularFile)) {
            stream.forEach(lib -> libs.append(this.gameDir.relativize(lib)).append(";"));
        }

        final var newScript = templateData.replace(matcher.group(1), libs).replace(matcher.group(2), mainClass);
        final var out = gameDir.resolve(name + ".bat");
        Files.writeString(out, newScript);

        out.toFile().setExecutable(true, false);
    }

    /*
     * Don't use this function by any means currently.
     * Linux servers don't need to edit any scripts.
     * Instead, only configs should be edited because start-server.sh acts as the bootstrapper.
     */
    private void createScriptLinux(String name, String mainClass, String templateData) throws IOException {
        var newScript = templateData;

        // Edit classpath
        {
            final var pattern = Pattern.compile("^(\\s*export\\s+LD_LIBRARY_PATH=\").+\"$",
                Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);

            final var matcher = pattern.matcher(templateData);
            if (!matcher.find()) {
                throw new RuntimeException("Failed to find match regex in launch script");
            }

            newScript = newScript.replace(matcher.group(1),
                matcher.group(1) + "${INSTDIR}/" + Utils.LEAF_FOLDER + "/libraries:");
        }

        final var out = gameDir.resolve(name + ".sh");
        Files.writeString(out, newScript);

        out.toFile().setExecutable(true, false);
    }
}
