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
package dev.aoqia.installer.util;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import dev.aoqia.installer.Main;
import dev.aoqia.installer.util.json.GameManifestVersion;

public class MetaHandler extends CompletableHandler<List<MetaHandler.GameVersion>> {
    private final String metaPath;
    private List<GameVersion> versions;

    public MetaHandler(String path) {
        this.metaPath = path;
    }

    public void load() throws IOException {
        final JsonNode versionTableNode = LeafService.queryMetaJson(metaPath);
        final JsonNode versionsNode = versionTableNode.path("versions");

        List<GameVersion> temp;
        if (versionsNode.isArray()) {
            final var versionsJson = Main.OBJECT_MAPPER.treeToValue(versionsNode,
                new TypeReference<List<GameManifestVersion>>() {});

            temp = versionsJson.stream()
                .map(GameVersion::new)
                .collect(Collectors.toList());
        } else {
            final var versionsJson = Main.OBJECT_MAPPER.treeToValue(versionsNode,
                new TypeReference<Map<String, Object>>() {});

            temp = new ArrayList<>();
            versionsJson.forEach((key, value) -> {
                temp.add(new GameVersion(key));
            });
        }
        this.versions = temp;

        complete(this.versions);
    }

    public List<GameVersion> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public GameVersion getLatestVersion(boolean unstable) {
        if (versions.isEmpty()) {
            throw new RuntimeException("no versions available at " + metaPath);
        }

        if (unstable) {
            for (GameVersion version : versions) {
                if (version.isUnstable()) {
                    return version;
                }
            }
        }

        return versions.get(0);
    }

    public static class GameVersion {
        String id;

        public GameVersion(String id) {
            this.id = id;
        }

        public GameVersion(GameManifestVersion version) {
            this.id = version.id;
        }

        public String id() {
            return id;
        }

        public boolean isUnstable() {
            return id.contains("unstable") || id.startsWith("0.");
        }
    }
}
