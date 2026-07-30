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
import java.util.zip.ZipEntry;
import java.util.zip.ZipFile;

import org.apache.commons.collections4.iterators.IteratorChain;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.node.ArrayNode;
import tools.jackson.databind.node.JsonNodeFactory;

import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.Main;
import dev.aoqia.leaf.installer.util.InstallerProgress;
import dev.aoqia.leaf.installer.util.LaunchConfigUtil;
import dev.aoqia.leaf.installer.util.LeafService;
import dev.aoqia.leaf.installer.util.Library;
import dev.aoqia.leaf.installer.util.Reference;
import dev.aoqia.leaf.installer.util.Utils;

public class ServerInstaller {
    private final Path gameDir;
    private final String gameVersion;
    private final Path libsDir;
    private final LoaderVersion loaderVersion;
    private final InstallerProgress progress;

    public ServerInstaller(Path gameDir, String gameVersion, LoaderVersion loaderVersion, InstallerProgress progress) {
        this.gameDir = gameDir;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
        this.progress = progress;

        this.libsDir = gameDir.resolve(Utils.LEAF_FOLDER).resolve("libraries");
    }

    public void install(boolean createConfig) throws IOException {
        System.out.printf("Installing %s with leaf %s%n", gameVersion, loaderVersion.name);

        progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.installing.server")).format(
            new Object[] { String.format("%s (%s)", loaderVersion.name, gameVersion) }));

        final var configName = String.format("leaf-%s-%s", loaderVersion.name, gameVersion);

        JsonNode loaderVersionJson;
        if (loaderVersion.path == null) {
            // Loader jar isn't custom, fetch json from GitHub.
            loaderVersionJson = LeafService.queryMetaJson("loader/%s.json".formatted(loaderVersion.name));
        } else {
            // Loader jar is locally available, fetch json from Jar.
            // Do this to prevent large GitHub traffic for dedicated servers.
            try (ZipFile zf = new ZipFile(loaderVersion.path.toFile())) {
                ZipEntry entry = zf.getEntry("leaf-installer.json");
                loaderVersionJson = Main.OBJECT_MAPPER.readTree(Utils.readString(zf.getInputStream(entry)));
            }
        }

        final var libsJson = loaderVersionJson.path("libraries");
        final var mainClass = loaderVersionJson.path("mainClass").path("server").asString();
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

        final var libs = new IteratorChain<>(libsJson.path("common").iterator(), libsJson.path("server").iterator());
        libs.forEachRemaining(json -> {
            Library library = new Library(json);
            Path libraryFile = libsDir.resolve("%s-%s.jar".formatted(library.artifactId, library.version));

            final var task = library.inputPath == null ? "copy" : "download";

            progress.updateProgress(
                new MessageFormat(Utils.BUNDLE.getString("progress.%s.library.entry".formatted(task))).format(
                    new Object[] { library.dependency }));

            try {
                if (library.inputPath == null) {
                    LeafService.downloadSubstitutedMaven(library.getURL(), libraryFile);
                } else {
                    Files.createDirectories(libraryFile.getParent());
                    Files.copy(library.inputPath, libraryFile, StandardCopyOption.REPLACE_EXISTING);
                }
            } catch (IOException e) {
                throw new RuntimeException("Failed to %s library %s".formatted(task, library.artifactId), e);
            }
        });

        final var launchConfigUtil = new LaunchConfigUtil(gameDir);
        if (createConfig) {
            launchConfigUtil.createConfig(configName, mainClassInternal);
        }
        launchConfigUtil.createScript(configName, mainClass);

        progress.updateProgress(Utils.BUNDLE.getString("progress.done"));
    }
}
