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
package dev.aoqia.installer;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.aoqia.installer.client.ClientHandler;
import dev.aoqia.installer.server.ServerHandler;
import dev.aoqia.installer.util.*;

public class Main {
    public static final List<Handler> HANDLERS = new ArrayList<>();
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public static MetaHandler GAME_VERSION_META;
    public static MetaHandler LOADER_META;

    public static void main(String[] args) throws IOException {

        if (OperatingSystem.CURRENT == OperatingSystem.WINDOWS) {
            // Use the operating system cert store
            System.setProperty("javax.net.ssl.trustStoreType", "WINDOWS-ROOT");
        }

        System.out.println("Loading Leaf Installer: " +
                           Main.class.getPackage().getImplementationVersion());

        HANDLERS.add(new ClientHandler());
        HANDLERS.add(new ServerHandler());

        ArgumentParser argumentParser = ArgumentParser.create(args);
        String command = argumentParser.getCommand().orElse(null);

        //Can be used if you wish to re-host or provide custom versions. Ensure you
        // include the trailing /
        String metaUrl =
            argumentParser.has("metaurl") ? argumentParser.get("metaurl") : null;
        String mavenUrl =
            argumentParser.has("mavenurl") ? argumentParser.get("mavenurl") : null;

        if (metaUrl != null || mavenUrl != null) {
            LeafService.setFixed(metaUrl, mavenUrl);
        }

        GAME_VERSION_META = new MetaHandler(
            "manifests/client/" + OperatingSystem.CURRENT.toShortString() +
            "/version_manifest.json");
        LOADER_META = new MetaHandler("loader_versions.json");

        //Default to the help command in a headless environment
        if (GraphicsEnvironment.isHeadless() && command == null) {
            command = "help";
        }

        if (command == null) {
            try {
                InstallerGui.start();
            } catch (Exception e) {
                e.printStackTrace();
                new CrashDialog(e);
            }
        } else if (command.equals("help")) {
            System.out.println("help - Opens this menu");
            HANDLERS.forEach(handler -> System.out.printf("%s %s\n",
                handler.name().toLowerCase(),
                handler.cliHelp()));
            loadMetadata();

            System.out.printf("\nLatest Version: %s\nLatest Loader: %s\n",
                GAME_VERSION_META.getLatestVersion(argumentParser.has("unstable")).id(),
                Main.LOADER_META.getLatestVersion(false).id());
        } else {
            loadMetadata();

            for (Handler handler : HANDLERS) {
                if (command.equalsIgnoreCase(handler.name())) {
                    try {
                        handler.installCli(argumentParser);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to install " + handler.name(),
                            e);
                    }

                    return;
                }
            }

            System.out.println("No handler found for " + args[0] + " see help");
        }
    }

    public static void loadMetadata() {
        try {
            LOADER_META.load();
            GAME_VERSION_META.load();
        } catch (Throwable t) {
            throw new RuntimeException("Unable to load metadata", t);
        }
    }
}
