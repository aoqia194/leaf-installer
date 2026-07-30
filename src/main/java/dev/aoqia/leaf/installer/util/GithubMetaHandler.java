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

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.JsonNode;

import dev.aoqia.leaf.installer.Main;
import dev.aoqia.leaf.installer.util.json.GitTreeObject;

/**
 * The primary use of this class is to parse git trees of a repository.
 * For example, this is used to get all the files in a specific folder under the leaf repository.
 */
public class GithubMetaHandler extends MetaHandler {
    private String[] subfolders;

    public GithubMetaHandler(String repoOwner, String repoName, String branch) {
        super("loader", String.format("%srepos/%s/%s/git/trees/%s", Reference.GITHUB_API, repoOwner, repoName, branch));
    }

    public GithubMetaHandler(String repoOwner, String repoName, String branch, String[] subfolders) {
        super("loader", String.format("%srepos/%s/%s/git/trees/%s", Reference.GITHUB_API, repoOwner, repoName, branch));
        this.subfolders = subfolders;
    }

    @Override
    public void load() throws IOException {
        JsonNode gitTreeNode = LeafService.queryJsonSubstitutedMaven(getMetaPath()).path("tree");

        // Resolve the subfolder tree if required, used to get files in loader/ folder.
        if (this.subfolders != null) {
            int subfoldersFound = 0;

            int i = 0;
            JsonNode node;
            while(i < gitTreeNode.size()) {
                node = gitTreeNode.get(i);

                if (node.path("type").asString().equals("tree")
                    && node.path("path").asString().equals(this.subfolders[subfoldersFound])) {
                    gitTreeNode = LeafService.queryJsonSubstitutedMaven(node.path("url").asString()).path("tree");
                    subfoldersFound++;
                    i = 0;
                }


                if (subfoldersFound > this.subfolders.length) {
                    break;
                }

                ++i;
            }
        }

        List<GameVersion> temp;
        final var versionsJson = Main.OBJECT_MAPPER.treeToValue(gitTreeNode,
            new TypeReference<List<GitTreeObject>>() {});

        temp = versionsJson.stream()
            .map(GameVersion::new)
            .sorted((v1, v2) -> v2.id.compareToIgnoreCase(v1.id))
            .collect(Collectors.toList());
        setVersions(temp);

        complete(getVersions());
    }
}
