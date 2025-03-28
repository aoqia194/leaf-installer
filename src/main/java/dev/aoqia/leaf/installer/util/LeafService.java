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
import java.net.URL;
import java.nio.file.Path;

import com.fasterxml.jackson.databind.JsonNode;
import dev.aoqia.leaf.installer.Main;

public final class LeafService {
    private static int activeIndex = 0; // index into INSTANCES or -1 if set to a fixed service
    private static LeafService fixedService;

    private final String meta;
    private final String maven;

    LeafService(String meta, String maven) {
        this.meta = meta;
        this.maven = maven;
    }

    /**
     * Query Leaf Meta path and decode as JSON.
     */
    public static JsonNode queryMetaJson(String path) throws IOException {
        return invokeWithFallbacks((service, arg) ->
            Main.OBJECT_MAPPER.readTree(Utils.readString(new URL(service.meta + arg))), path);
    }

    /**
     * Query and decode JSON from url, substituting Fabric Maven with fallbacks or overrides.
     */
    public static JsonNode queryJsonSubstitutedMaven(String url) throws IOException {
        if (!url.startsWith(Reference.DEFAULT_MAVEN_SERVER)) {
            return Main.OBJECT_MAPPER.readTree(Utils.readString(new URL(url)));
        }

        String path = url.substring(Reference.DEFAULT_MAVEN_SERVER.length());

        return invokeWithFallbacks((service, arg) ->
            Main.OBJECT_MAPPER.readTree(Utils.readString(new URL(service.maven + arg))), path);
    }

    /**
     * Download url to file, substituting Fabric Maven with fallbacks or overrides.
     */
    public static void downloadSubstitutedMaven(String url, Path out) throws IOException {
        if (!url.startsWith(Reference.DEFAULT_MAVEN_SERVER)) {
            Utils.downloadFile(new URL(url), out);
            return;
        }

        String path = url.substring(Reference.DEFAULT_MAVEN_SERVER.length());

        invokeWithFallbacks((service, arg) -> {
            Utils.downloadFile(new URL(service.maven + arg), out);
            return null;
        }, path);
    }

    private static <A, R> R invokeWithFallbacks(Handler<A, R> handler, A arg) throws IOException {
        if (fixedService != null) {
            return handler.apply(fixedService, arg);
        }

        int index = activeIndex;
        IOException exc = null;

        do {
            LeafService service = Reference.LEAF_SERVICES[index];

            try {
                R ret = handler.apply(service, arg);
                activeIndex = index;

                return ret;
            } catch (IOException e) {
                System.out.println("service " + service + " failed: " + e);

                if (exc == null) {
                    exc = e;
                } else {
                    exc.addSuppressed(e);
                }
            }

            index = (index + 1) % Reference.LEAF_SERVICES.length;
        } while (index != activeIndex);

        throw exc;
    }

    /**
     * Configure fixed service urls, disabling fallbacks or the defaults.
     */
    public static void setFixed(String metaUrl, String mavenUrl) {
        if (metaUrl == null && mavenUrl == null) {
            throw new NullPointerException("both meta and maven are null");
        }

        if (metaUrl == null) {
            metaUrl = Reference.DEFAULT_META_SERVER;
        }
        if (mavenUrl == null) {
            mavenUrl = Reference.DEFAULT_MAVEN_SERVER;
        }

        activeIndex = -1;
        fixedService = new LeafService(metaUrl, mavenUrl);
    }

    public String getMetaUrl() {
        return meta;
    }

    public String getMavenUrl() {
        return maven;
    }

    @Override
    public String toString() {
        return "LeafService{"
               + "meta='" + meta + '\''
               + ", maven='" + maven + "'}";
    }

    private interface Handler<A, R> {
        R apply(LeafService service, A arg) throws IOException;
    }
}
