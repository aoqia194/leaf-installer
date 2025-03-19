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
package dev.aoqia.installer.client;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.text.MessageFormat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import dev.aoqia.installer.LoaderVersion;
import dev.aoqia.installer.Main;
import dev.aoqia.installer.util.*;

public class ClientInstaller {
    public static String install(Path gameDir,
        String gameVersion,
        LoaderVersion loaderVersion,
        boolean createProfile,
        InstallerProgress progress) throws IOException {
        System.out.println(
            "Installing " + gameVersion + " with leaf " + loaderVersion.name);

        String configName = String.format("%s-%s-%s", Reference.LOADER_NAME,
            loaderVersion.name, gameVersion);

        JsonNode loaderVersionJson = LeafService.queryMetaJson("loader_versions.json")
            .path("versions")
            .path(loaderVersion.name);

        // Clone default bootstrapper config and load it.
        // TODO: Handle other OS.
        if (createProfile) {
            Path bootstrapperConfig = gameDir.resolve(configName + ".json");
            Path origConfig;
            if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
                origConfig = gameDir.resolve("ProjectZomboid64.json");
            } else {
                throw new RuntimeException(
                    "Multi-OS bootstrapper config not implemented yet");
            }

            if (Files.exists(bootstrapperConfig)) {
                throw new RuntimeException(
                    "Bootstrapper config %s already exists.".formatted(
                        bootstrapperConfig));
            }
            Files.copy(origConfig, bootstrapperConfig);

            // Load version config and modify cloned bootstrapper config, then save to new
            // config.
            JsonNode bootstrapperConfigJson = Main.OBJECT_MAPPER.readTree(
                Utils.readString(bootstrapperConfig));
            ((ObjectNode) bootstrapperConfigJson).setAll(
                (ObjectNode) loaderVersionJson.path("config"));

            // Always remove these stupid JVM properties that shouldn't exist.
            ArrayNode vmArgs = (ArrayNode) bootstrapperConfigJson.path("vmArgs");
            assert vmArgs.isArray();
            for (int i = 0; i < vmArgs.size(); ++i) {
                final var node = vmArgs.get(i);
                if (node.asText().startsWith("-Xms") ||
                    node.asText().startsWith("-Xmx")) {
                    vmArgs.remove(i);
                }
            }

            Files.writeString(bootstrapperConfig, bootstrapperConfigJson.toString());
        }

        for (var libraryJson : loaderVersionJson.path("libraries")) {
            Library library = new Library(libraryJson);
            Path libraryFile = gameDir.resolve(library.getPath());
            String url = library.getURL();

            //System.out.println("Downloading "+url+" to "+libraryFile);
            progress.updateProgress(new MessageFormat(
                Utils.BUNDLE.getString("progress.download.library.entry")).format(
                new Object[] { library.name }));
            LeafService.downloadSubstitutedMaven(url, libraryFile);
        }

        progress.updateProgress(Utils.BUNDLE.getString("progress.done"));

        return configName;
    }
}
