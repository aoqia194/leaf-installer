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
public final class LauncherConfig {
    private String mainClass;
    private List<String> vmArgs;
    private List<String> classpath;

    public LauncherConfig(String mainClass, List<String> vmArgs, List<String> classpath) {
        this.mainClass = mainClass;
        this.vmArgs = vmArgs;
        this.classpath = classpath;
    }

    public String getMainClass() {
        return mainClass;
    }

    public void setMainClass(String mainClass) {
        this.mainClass = mainClass;
    }

    public List<String> getVmArgs() {
        return vmArgs;
    }

    public void setVmArgs(List<String> vmArgs) {
        this.vmArgs = vmArgs;
    }

    public List<String> getClasspath() {
        return classpath;
    }

    public void setClasspath(List<String> classpath) {
        this.classpath = classpath;
    }
}
