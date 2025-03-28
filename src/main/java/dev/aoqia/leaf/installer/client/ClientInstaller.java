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
import java.util.stream.Stream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.JsonNodeFactory;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.Main;
import dev.aoqia.leaf.installer.util.*;
import org.apache.commons.collections4.iterators.IteratorChain;

public class ClientInstaller {
    public static String install(Path gameDir, String gameVersion, LoaderVersion loaderVersion,
        boolean createProfile, InstallerProgress progress) throws IOException {
        System.out.println("Installing " + gameVersion + " with leaf " + loaderVersion.name);

        String configName = String.format("%s-%s-%s", Reference.LOADER_NAME,
            loaderVersion.name, gameVersion);
        JsonNode loaderVersionJson = LeafService.queryMetaJson(
            "loader/" + loaderVersion.name + ".json");

        // Download libraries before creating profile.
        final Path libsDir = gameDir.resolve(".leaf/libraries");
        final var libsJson = loaderVersionJson.path("libraries");

        // Putting loader dependency into the libs list for later download.
        final var obj = JsonNodeFactory.instance.objectNode();
        obj.put("name", "dev.aoqia.leaf:loader:" + loaderVersion.name);
        obj.put("url", Reference.DEFAULT_MAVEN_SERVER);
        ((ArrayNode) libsJson.path("common")).add(obj);

        final var libs = new IteratorChain<>(libsJson.path("common").iterator(),
            libsJson.path("client").iterator());

        libs.forEachRemaining((libJson) -> {
            Library library = new Library(libJson);
            Path libraryFile = libsDir.resolve(
                "%s-%s.jar".formatted(library.artifactId, library.version));
            String url = library.getURL();

            // System.out.println("Downloading "+url+" to "+libraryFile);
            progress.updateProgress(new MessageFormat(
                Utils.BUNDLE.getString("progress.download.library.entry"))
                .format(new Object[] { library.dependency }));

            try {
                LeafService.downloadSubstitutedMaven(url, libraryFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to download library " + library.artifactId, e);
            }
        });

        // Clone default bootstrapper config and load it.
        // TODO: Handle other OS? Maybe not needed?
        if (createProfile) {
            Path bootstrapperConfig = gameDir.resolve(configName + ".json");
            Path origConfig = gameDir.resolve("ProjectZomboid64.json");

            if (Files.exists(bootstrapperConfig)) {
                throw new RuntimeException(
                    "Bootstrapper config %s already exists.".formatted(
                        bootstrapperConfig));
            }
            Files.copy(origConfig, bootstrapperConfig);

            // Load version config and modify cloned bootstrapper config, then save to new config.
            JsonNode bootstrapperConfigJson = Main.OBJECT_MAPPER.readTree(
                Files.readString(bootstrapperConfig));
            ((ObjectNode) bootstrapperConfigJson).put("mainClass",
                loaderVersionJson.path("mainClass").path("client").asText().replace(".", "/"));

            // Always remove these stupid JVM properties that shouldn't exist.
            final ArrayNode vmArgs = (ArrayNode) bootstrapperConfigJson.path("vmArgs");
            assert vmArgs.isArray();
            for (int i = 0; i < vmArgs.size(); ++i) {
                final var node = vmArgs.get(i);
                if (node.asText().startsWith("-Xms") ||
                    node.asText().startsWith("-Xmx") ||
                    node.asText().equals("-Djava.awt.headless=true")) {
                    vmArgs.remove(i);
                    i--;
                }
            }

            // Add the gameVersion property to the vmArgs so loader knows what version it is.
            // Probably a better way to do it but idk.
            vmArgs.add("-Dleaf.gameVersion=" + gameVersion);

            // Add our loader's libraries to the classpath.
            // Java 6+ supports cp wildcards but the bootstrapper hard crashes with them.
            final ArrayNode classpath = (ArrayNode) bootstrapperConfigJson.path("classpath");
            try (final Stream<Path> stream = Files.walk(libsDir).filter(Files::isRegularFile)) {
                stream.forEach(path -> classpath.add(gameDir.relativize(path).toString()));
            }

            Files.writeString(bootstrapperConfig, bootstrapperConfigJson.toString());
        }

        progress.updateProgress(Utils.BUNDLE.getString("progress.done"));
        return configName;
    }
}
