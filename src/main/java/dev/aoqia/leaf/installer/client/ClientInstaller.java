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

import org.apache.commons.collections4.iterators.IteratorChain;

import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.util.InstallerProgress;
import dev.aoqia.leaf.installer.util.LeafService;
import dev.aoqia.leaf.installer.util.Library;
import dev.aoqia.leaf.installer.util.Reference;
import dev.aoqia.leaf.installer.util.Utils;
import dev.aoqia.leaf.installer.util.json.LoaderJson;

public class ClientInstaller {
    private final Path gameDir;
    private final String gameVersion;
    private final Path libsDir;
    private final LoaderVersion loaderVersion;
    private final InstallerProgress progress;

    public ClientInstaller(Path gameDir, String gameVersion, InstallerProgress progress) {
        this.gameDir = gameDir;
        this.gameVersion = gameVersion;
        this.loaderVersion = null;
        this.progress = progress;

        this.libsDir = gameDir.resolve(".leaf/libraries");
    }

    public ClientInstaller(Path gameDir, String gameVersion, LoaderVersion loaderVersion, InstallerProgress progress) {
        this.gameDir = gameDir;
        this.gameVersion = gameVersion;
        this.loaderVersion = loaderVersion;
        this.progress = progress;

        this.libsDir = gameDir.resolve(".leaf/libraries");
    }

    public String install(boolean proxy) throws IOException {
        System.out.printf("Installing %s with leaf %s%n", gameVersion, loaderVersion.name);

        String configName = String.format("leaf-%s-%s", loaderVersion.name, gameVersion);
        LoaderJson loaderVersionJson = LeafService.queryMetaJson("loader/%s.json".formatted(loaderVersion.name),
            LoaderJson.class);
        LoaderJson.Libraries libsJson = loaderVersionJson.libraries();

        Files.createDirectories(this.libsDir);

        // Putting loader dependency into the libs list for later download.
        libsJson
            .common()
            .add(new LoaderJson.Library("dev.aoqia.leaf:loader:" + loaderVersion.name, Reference.DEFAULT_MAVEN_SERVER,
                null, null, null, null, null));

        final var libs = new IteratorChain<>(libsJson.common().iterator(), libsJson.client().iterator());

        libs.forEachRemaining(libJson -> {
            Library library = new Library(libJson);
            Path libraryFile = libsDir.resolve("%s-%s.jar".formatted(library.artifactId, library.version));
            String url = library.getURL();

            progress.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.download.library.entry")).format(
                new Object[] { library.dependency }));

            try {
                LeafService.downloadSubstitutedMaven(url, libraryFile);
            } catch (IOException e) {
                throw new RuntimeException("Failed to download library " + library.artifactId, e);
            }
        });

        progress.updateProgress(Utils.BUNDLE.getString("progress.done"));
        return configName;
    }
}
