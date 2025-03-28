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
import java.util.List;

import com.fasterxml.jackson.databind.JsonNode;

public record LauncherMeta(List<Version> versions) {
    private static LauncherMeta launcherMeta = null;

    public static LauncherMeta getLauncherMeta() throws IOException {
        if (launcherMeta == null) {
            launcherMeta = load();
        }

        return launcherMeta;
    }

    private static LauncherMeta load() throws IOException {
        List<Version> versions = new ArrayList<>(
            getVersionsFromUrl(Reference.ZOMBOID_VERSION_MANIFEST));
        return new LauncherMeta(versions);
    }

    private static List<Version> getVersionsFromUrl(String url) throws IOException {
        JsonNode json = LeafService.queryJsonSubstitutedMaven(url);

        List<Version> versions = new ArrayList<>();
        json.path("versions").forEach(version -> versions.add(new Version(version)));
        return versions;
    }

    public Version getVersion(String version) {
        return versions.stream().filter(v -> v.id.equals(version)).findFirst().orElse(null);
    }

    public static class Version {
        public final String id;
        public final String url;

        private VersionMeta versionMeta = null;

        public Version(JsonNode json) {
            this.id = json.path("id").textValue();
            this.url = json.path("url").textValue();
        }

        public VersionMeta getVersionMeta() throws IOException {
            if (versionMeta == null) {
                JsonNode json = LeafService.queryJsonSubstitutedMaven(url);
                versionMeta = new VersionMeta(json);
            }

            return versionMeta;
        }
    }
}
