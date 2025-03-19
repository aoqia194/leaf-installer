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

import java.io.File;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;

public class Library {
    public final String dependency;
    public final String url;
    public final Path inputPath;
    public String groupId;
    public String artifactId;
    public String version;

    public Library(String dependency, String url, Path inputPath) {
        this.dependency = dependency;
        this.url = url;
        this.inputPath = inputPath;

        final var parts = dependency.split(":", 3);
        this.groupId = parts[0];
        this.artifactId = parts[1];
        this.version = parts[2];
    }

    public Library(JsonNode json) {
        dependency = json.path("name").asText();
        url = json.path("url").asText();
        inputPath = null;

        final var parts = dependency.split(":", 3);
        this.groupId = parts[0];
        this.artifactId = parts[1];
        this.version = parts[2];
    }

    public String getURL() {
        return url + "%s/%s/%s/%s-%s.jar".formatted(groupId.replace(".", "/"), artifactId, version,
            artifactId, version);
    }

    public String getPath() {
        return "%s/%s/%s/%s-%s.jar".formatted(groupId.replace(".", "/"), artifactId, version,
            artifactId, version).replaceAll(" ", "_").replaceAll("/", File.separator);
    }
}
