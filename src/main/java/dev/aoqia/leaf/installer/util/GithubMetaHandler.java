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
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import dev.aoqia.leaf.installer.Main;
import dev.aoqia.leaf.installer.util.json.GitTreeObject;

/**
 * The primary use of this class is to parse git trees of a repository. For example, this is used to
 * get all of the files in a specific folder under the leaf repository.
 */
public class GithubMetaHandler extends MetaHandler {
    private String subfolder;

    public GithubMetaHandler(String repoOwner, String repoName, String branch) {
        super(String.format("%srepos/%s/%s/git/trees/%s", Reference.GITHUB_API, repoOwner, repoName,
            branch));
    }

    public GithubMetaHandler(String repoOwner, String repoName, String branch, String subfolder) {
        super(String.format("%srepos/%s/%s/git/trees/%s", Reference.GITHUB_API, repoOwner, repoName,
            branch));
        this.subfolder = subfolder;
    }

    @Override
    public void load() throws IOException {
        JsonNode gitTreeNode = LeafService.queryJsonSubstitutedMaven(this.metaPath).path("tree");

        // Resolve the subfolder tree if required, used to get files in loader/ folder.
        if (this.subfolder != null) {
            for (final JsonNode node : gitTreeNode) {
                if (!node.path("type").asText().equals("tree") ||
                    !node.path("path").asText().equals(this.subfolder)) {
                    continue;
                }

                gitTreeNode = LeafService.queryJsonSubstitutedMaven(node.path("url").asText())
                    .path("tree");
                break;
            }
        }

        List<ComponentVersion> temp;
        final var versionsJson = Main.OBJECT_MAPPER.treeToValue(gitTreeNode,
            new TypeReference<List<GitTreeObject>>() {});

        temp = versionsJson.stream()
            .map(ComponentVersion::new)
            .collect(Collectors.toList());
        this.versions = temp;

        complete(this.versions);
    }
}
