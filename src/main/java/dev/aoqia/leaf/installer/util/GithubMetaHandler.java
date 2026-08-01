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
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import dev.aoqia.leaf.installer.util.json.GitTree;

/**
 * The primary use of this class is to parse git trees of a repository.
 * For example, this is used to get all the files in a specific folder under the leaf repository.
 */
public class GithubMetaHandler extends LoaderMetaHandler {
    private String[] subfolders;

    public GithubMetaHandler(String repoOwner, String repoName, String branch) {
        super(String.format("%srepos/%s/%s/git/trees/%s", Reference.GITHUB_API, repoOwner, repoName, branch));
    }

    public GithubMetaHandler(String repoOwner, String repoName, String branch, String[] subfolders) {
        super(String.format("%srepos/%s/%s/git/trees/%s", Reference.GITHUB_API, repoOwner, repoName, branch));
        this.subfolders = subfolders;
    }

    @Override
    public void load() throws IOException {
        GitTree.GitTreeObject[] tree = LeafService.queryJsonSubstitutedMaven(getMetaPath(), GitTree.class).tree();

        // Resolve the subfolder tree if required, used to get files in loader/ folder.
        if (this.subfolders != null) {
            int subfoldersFound = 0;

            int i = 0;
            while (i < tree.length) {
                GitTree.GitTreeObject node = tree[i];

                if (node.type().equals("tree") && node.path().equals(this.subfolders[subfoldersFound])) {
                    tree = LeafService.queryJsonSubstitutedMaven(node.url(), GitTree.class).tree();
                    subfoldersFound++;
                    i = 0;
                }


                if (subfoldersFound > this.subfolders.length) {
                    break;
                }

                ++i;
            }
        }

        List<Version> temp;

        temp = Arrays
            .stream(tree)
            .map(Version::new)
            .sorted((v1, v2) -> v2.id().compareToIgnoreCase(v1.id()))
            .collect(Collectors.toList());
        setVersions(temp);

        complete(getVersions());
    }
}
