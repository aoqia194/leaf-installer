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
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import dev.aoqia.leaf.installer.util.json.GameManifestVersion;
import dev.aoqia.leaf.installer.util.json.VersionTable;

public class GameMetaHandler extends MetaHandler<GameMetaHandler.Version> {
    private List<Version> versions;

    public GameMetaHandler(String name, String metaPath) {
        super(name, metaPath);
    }

    @Override
    public void load() throws IOException {
        VersionTable versionTable = LeafService.queryMetaJson(getMetaPath(), VersionTable.class);
        Object versions = versionTable.versions();

        List<Version> temp;
        if (versions instanceof List) {
            temp = versionTable.getVersionsAsList();
        } else if (versions instanceof Map) {
            temp = new ArrayList<>();
            versionTable.getVersionsAsMap().forEach((key, value) -> temp.add((Version) value));
        } else {
            throw new IllegalStateException("Exploded!");
        }

        this.versions = temp;
        complete(this.versions);
    }

    public Version getLatestVersion(boolean unstable) {
        if (versions.isEmpty()) {
            throw new RuntimeException("no versions available at " + getMetaPath());
        }

        if (unstable) {
            for (Version version : versions) {
                if (version.isUnstable()) {
                    return version;
                }
            }
        }

        return versions.get(0);
    }

    public Version parseVersion(String value, boolean unstable) {
        if (value == null || value.isEmpty() || value.equalsIgnoreCase("latest")) {
            return getLatestVersion(unstable);
        } else {
            for (Version version : versions) {
                if (version.id.equals(value)) {
                    return version;
                }
            }

            return null;
        }
    }

    public List<Version> getVersions() {
        return Collections.unmodifiableList(versions);
    }

    public record Version(String id) {
        public Version(GameManifestVersion version) {
            this(version.id);
        }

        public boolean isUnstable() {
            return id.contains("-unstable");
        }
    }
}
