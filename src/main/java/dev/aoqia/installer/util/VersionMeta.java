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

import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.databind.JsonNode;

public class VersionMeta {
    public final String id;
    public final Map<String, Download> downloads;

    public VersionMeta(JsonNode json) {
        id = json.path("id").textValue();
        downloads = new HashMap<>();

        json.path("downloads").fields().forEachRemaining(entry -> {
            downloads.put(entry.getKey(), new Download(entry.getValue()));
        });
    }

    public static class Download {
        public final String sha1;
        public final long size;
        public final String url;

        public Download(JsonNode json) {
            sha1 = json.path("sha1").textValue();
            size = json.path("size").longValue();
            url = json.path("url").textValue();
        }
    }
}
