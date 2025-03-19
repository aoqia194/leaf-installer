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
package dev.aoqia.leaf.installer.server;

import javax.swing.*;
import java.awt.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import dev.aoqia.leaf.installer.Handler;
import dev.aoqia.leaf.installer.InstallerGui;
import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.util.ArgumentParser;
import dev.aoqia.leaf.installer.util.InstallerProgress;
import dev.aoqia.leaf.installer.util.Utils;

public class ServerHandler extends Handler {
    @Override
    public String name() {
        return "Server";
    }

    @Override
    public void install() {
        String gameVersion = (String) gameVersionComboBox.getSelectedItem();
        LoaderVersion loaderVersion = queryLoaderVersion();
        if (loaderVersion == null) {
            return;
        }

        new Thread(() -> {
            try {
                ServerInstaller.install(Paths.get(installLocation.getText()).toAbsolutePath(),
                    loaderVersion,
                    gameVersion,
                    this);
                ServerPostInstallDialog.show(this);
            } catch (Exception e) {
                error(e);
            }

            buttonInstall.setEnabled(true);
        }).start();
    }

    @Override
    public void installCli(ArgumentParser args) throws Exception {
        Path dir = Paths.get(args.getOrDefault("dir", () -> ".")).toAbsolutePath().normalize();

        if (!Files.isDirectory(dir)) {
            throw new FileNotFoundException("Server directory not found at " + dir + " or not a directory");
        }

        LoaderVersion loaderVersion = new LoaderVersion(getLoaderVersion(args));
        String gameVersion = getGameVersion(args);
        ServerInstaller.install(dir, loaderVersion, gameVersion, InstallerProgress.CONSOLE);
        InstallerProgress.CONSOLE.updateProgress(new MessageFormat(Utils.BUNDLE.getString("progress.done.start" +
                                                                                          ".server")).format(
            new Object[] { ServerInstaller.DEFAULT_LAUNCH_JAR_NAME }));
    }

	@Override
	public String cliHelp() {
		return "-dir <install dir, default current dir> -pzversion <zomboid version, default latest> -loader <loader version, default latest>";
	}

	@Override
	public void setupPane1(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
		if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
			return;
		}
    }

    @Override
    public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
        installLocation.setText(Paths.get(".").toAbsolutePath().normalize().toString());
    }
}
