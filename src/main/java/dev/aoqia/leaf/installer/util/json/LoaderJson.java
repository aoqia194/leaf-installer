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
package dev.aoqia.leaf.installer.util.json;

import java.util.List;

import com.dslplatform.json.CompiledJson;

@CompiledJson
public record LoaderJson(Libraries libraries, MainClass mainClass) {
    @CompiledJson
    public record Libraries(
        List<Library> client,
        List<Library> common,
        List<Library> server,
        List<Library> development) {}

    @CompiledJson
    public record Library(String name, String url, String md5, String sha1, String sha256, String sha512, Long size) {}

    @CompiledJson
    public record MainClass(String client, String server) {}
}
