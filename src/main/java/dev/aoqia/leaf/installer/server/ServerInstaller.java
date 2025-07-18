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
package dev.aoqia.leaf.installer.server;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.text.MessageFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.Main;
import dev.aoqia.leaf.installer.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.iterators.IteratorChain;

public class ServerInstaller {
    private final Path gameDir;
    private final String gameVersion;
    private final Path libsDir;
    private final LoaderVersion loaderVersion;
    private final InstallerProgress progress;

    public ServerInstaller(Path gameDir, String gameVersion, LoaderVersion loaderVersion,
        InstallerProgress progress) {
        this.gameDir = gameDir;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
        this.progress = progress;

        this.libsDir = gameDir.resolve(".leaf/libraries");
    }

    public void install(boolean createConfig) throws IOException {
        System.out.printf("Installing %s with leaf %s%n", gameVersion, loaderVersion.name);

        progress.updateProgress(
            new MessageFormat(Utils.BUNDLE.getString("progress.installing.server")).format(
                new Object[] { String.format("%s (%s)", loaderVersion.name, gameVersion) }));

        final var configName = String.format("leaf-%s-%s", loaderVersion.name, gameVersion);

        JsonNode loaderVersionJson;
        if (loaderVersion.path == null) {
            // Loader jar isn't custom, fetch json from GitHub.
            loaderVersionJson = LeafService.queryMetaJson(
                "loader/%s.json".formatted(loaderVersion.name));
        } else {
            // Loader jar is locally available, fetch json from Jar.
            // Do this to prevent large GitHub traffic for dedicated servers.
            try (ZipFile zf = new ZipFile(loaderVersion.path.toFile())) {
                ZipEntry entry = zf.getEntry("leaf-installer.json");
                loaderVersionJson = Main.OBJECT_MAPPER.readTree(
                    Utils.readString(zf.getInputStream(entry)));
            }
        }

        final var libsJson = loaderVersionJson.path("libraries");
        final var mainClass = loaderVersionJson.path("mainClass")
            .path("server")
            .asText();
        final var mainClassInternal = mainClass.replace(".", "/");
        Files.createDirectories(libsDir);

        // Putting the loader itself into the libs list to download/copy later.
        final var obj = JsonNodeFactory.instance.objectNode();
        obj.put("name", "dev.aoqia.leaf:loader:" + loaderVersion.name);
        if (loaderVersion.path == null) {
            obj.put("url", Reference.DEFAULT_MAVEN_SERVER);
        } else {
            obj.put("path", loaderVersion.path.toUri().toString());
        }
        ((ArrayNode) libsJson.path("common")).add(obj);

        final var libs = new IteratorChain<>(libsJson.path("common").iterator(),
            libsJson.path("server").iterator());
        libs.forEachRemaining(json -> {
            Library library = new Library(json);
            Path libraryFile = libsDir.resolve(
                "%s-%s.jar".formatted(library.artifactId, library.version));

            final var task = library.inputPath == null ? "copy" : "download";

            progress.updateProgress(new MessageFormat(
                Utils.BUNDLE.getString("progress.%s.library.entry".formatted(task))).format(
                new Object[] { library.dependency }));

            try {
                if (library.inputPath == null) {
                    LeafService.downloadSubstitutedMaven(library.getURL(), libraryFile);
                } else {
                    Files.createDirectories(libraryFile.getParent());
                    Files.copy(library.inputPath, libraryFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException(
                    "Failed to %s library %s".formatted(task, library.artifactId), e);
            }
        });

        if (createConfig) {
            createNewConfig(configName, mainClassInternal);
        }
        createLaunchScript(configName, mainClass);

        progress.updateProgress(Utils.BUNDLE.getString("progress.done"));
    }

    private void createNewConfig(String configName, String mainClass) throws IOException {
        Path origConfig = gameDir.resolve("ProjectZomboid64.json");
        if (Files.notExists(origConfig)) {
            throw new RuntimeException(
                Utils.BUNDLE.getString("progress.exception.no.launcher.config"));
        }

        Path bootstrapperConfig = gameDir.resolve(configName + ".json");
        if (Files.exists(bootstrapperConfig)) {
            throw new RuntimeException(
                "Bootstrapper config %s already exists.".formatted(
                    bootstrapperConfig));
        }

        try {
            Files.copy(origConfig, bootstrapperConfig);
        } catch (IOException e) {
            throw new RuntimeException("Failed to copy original bootstrapper config: ", e);
        }

        // Load version config and modify cloned bootstrapper config, then save to new config.
        JsonNode bootstrapperConfigJson;
        try {
            bootstrapperConfigJson = Main.OBJECT_MAPPER.readTree(
                Files.readString(bootstrapperConfig));
        } catch (IOException e) {
            throw new RuntimeException("Failed to read bootstrapper config: ", e);
        }
        ((ObjectNode) bootstrapperConfigJson).put("mainClass", mainClass);

        // Always remove these stupid JVM properties that shouldn't exist.
        final ArrayNode vmArgs = (ArrayNode) bootstrapperConfigJson.path("vmArgs");
        assert vmArgs.isArray();
        for (int i = 0; i < vmArgs.size(); ++i) {
            final var node = vmArgs.get(i);
            if (node.asText().startsWith("-Xms") ||
                node.asText().startsWith("-Xmx")
                // node.asText().equals("-Djava.awt.headless=true")
            ) {
                vmArgs.remove(i--);
            }
        }

        // Add our loader's libraries to the classpath.
        // Java 6+ supports cp wildcards but the bootstrapper hard crashes with them.
        final ArrayNode classpath = (ArrayNode) bootstrapperConfigJson.path("classpath");
        try (final Stream<Path> stream = Files.walk(libsDir).filter(Files::isRegularFile)) {
            stream.forEach(path -> classpath.add(gameDir.relativize(path).toString()));
        }

        try {
            Files.writeString(bootstrapperConfig, bootstrapperConfigJson.toString());
        } catch (IOException e) {
            throw new RuntimeException("Failed to write bootstrapper config: ", e);
        }
    }

    private void createLaunchScript(String configName, String mainClass) throws IOException {
        final var in = gameDir.resolve("StartServer64.bat");
        if (Files.notExists(in)) {
            throw new RuntimeException("Server launch script does not exist.");
        }

        String newScript = Files.readString(in);

        // Really dodgy way of doing it, probably breaks in the future.

        Pattern p = Pattern.compile("^SET\\s*PZ_CLASSPATH=(.+)[\\r\\n]+.*-cp.*(zombie(?:\\.\\w+)+)",
            Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
        Matcher m = p.matcher(newScript);
        if (m.find() && m.groupCount() == 2) {
            StringBuilder libs = new StringBuilder(m.group(1));
            int i = 0;
            try (final var stream = Files.walk(libsDir).filter(Files::isRegularFile)) {
                for (final var lib : stream.toList()) {
                    final var libStr = gameDir.relativize(lib) + ";";
                    libs.insert(i, libStr);
                    i += libStr.length();
                }
            }
            newScript = newScript.replace(m.group(1), libs).replace(m.group(2), mainClass);
        } else {
            throw new RuntimeException("Failed to find match regex in launch script");
        }

        final var out = gameDir.resolve(configName + ".bat");

        Files.writeString(out, newScript);
        out.toFile().setExecutable(true, false);
    }
}
