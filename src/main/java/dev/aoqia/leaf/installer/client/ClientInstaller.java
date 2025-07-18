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
package dev.aoqia.leaf.installer.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.Main;
import dev.aoqia.leaf.installer.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import org.apache.commons.collections4.iterators.IteratorChain;

public class ClientInstaller {
    private final Path gameDir;
    private final String gameVersion;
    private final Path libsDir;
    private final LoaderVersion loaderVersion;
    private final InstallerProgress progress;

    public ClientInstaller(Path gameDir, String gameVersion, LoaderVersion loaderVersion,
        InstallerProgress progress) {
        this.gameDir = gameDir;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
        this.progress = progress;

        this.libsDir = gameDir.resolve(".leaf/libraries");
    }

    public String install(boolean createConfig) throws IOException {
        System.out.printf("Installing %s with leaf %s%n", gameVersion, loaderVersion.name);

        String configName = String.format("leaf-%s-%s", loaderVersion.name, gameVersion);
        JsonNode loaderVersionJson = LeafService.queryMetaJson(
            "loader/%s.json".formatted(loaderVersion.name));

        final var libsJson = loaderVersionJson.path("libraries");
        final var mainClass = loaderVersionJson.path("mainClass")
            .path("client")
            .asText()
            .replace(".", "/");
        Files.createDirectories(this.libsDir);

        // Putting loader dependency into the libs list for later download.
        final var obj = JsonNodeFactory.instance.objectNode();
        obj.put("name", "dev.aoqia.leaf:loader:" + loaderVersion.name);
        obj.put("url", Reference.DEFAULT_MAVEN_SERVER);
        ((ArrayNode) libsJson.path("common")).add(obj);

        final var libs = new IteratorChain<>(libsJson.path("common").iterator(),
            libsJson.path("client").iterator());

        libs.forEachRemaining(libJson -> {
            Library library = new Library(libJson);
            Path libraryFile = libsDir.resolve(
                "%s-%s.jar".formatted(library.artifactId, library.version));
            String url = library.getURL();

            progress.updateProgress(new MessageFormat(
                Utils.BUNDLE.getString("progress.download.library.entry"))
                .format(new Object[] { library.dependency }));

            try {
                LeafService.downloadSubstitutedMaven(url, libraryFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to download library " + library.artifactId, e);
            }
        });

        // TODO: Handle other OS configs.
        if (createConfig) {
            createNewConfig(configName, mainClass);
        }

        progress.updateProgress(Utils.BUNDLE.getString("progress.done"));
        return configName;
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
        Files.copy(origConfig, bootstrapperConfig);

        // Load version config and modify cloned bootstrapper config, then save to new config.
        JsonNode bootstrapperConfigJson = Main.OBJECT_MAPPER.readTree(
            Files.readString(bootstrapperConfig));
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
        int i = 0;
        final ArrayNode classpath = (ArrayNode) bootstrapperConfigJson.path("classpath");
        try (final var stream = Files.walk(libsDir).filter(Files::isRegularFile)) {
            for (final var lib : stream.toList()) {
                classpath.insert(i, gameDir.relativize(lib).toString());
                i++;
            }
        }

        Files.writeString(bootstrapperConfig, bootstrapperConfigJson.toString());
    }
}
