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

import java.awt.*;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import dev.aoqia.leaf.installer.Handler;
import dev.aoqia.leaf.installer.InstallerGui;
import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.util.ArgumentParser;
import dev.aoqia.leaf.installer.util.InstallerProgress;
import dev.aoqia.leaf.installer.util.OperatingSystem;
import dev.aoqia.leaf.installer.util.Utils;

import javax.swing.*;

public class ServerHandler extends Handler {
    public JCheckBox createScriptCheckbox;

    @Override
    public String name() {
        return Utils.BUNDLE.getString("tab.server");
    }

    @Override
    public void install(boolean proxy) {
        installInternal(proxy);
    }

    private void installInternal(boolean proxy) {
        String gameVersion = (String) gameVersionComboBox.getSelectedItem();
        boolean createConfig = createConfigCheckbox.isEnabled() && createConfigCheckbox.isSelected();
        boolean createScript = createScriptCheckbox.isEnabled() && createScriptCheckbox.isSelected();

        final LoaderVersion loaderVersion;
        if (!proxy) {
            loaderVersion = queryLoaderVersion();
        } else {
            try {
                loaderVersion = new LoaderVersion(Utils.getLatestLoaderProxy().version);
            } catch (IOException exc) {
                error(exc);
                return;
            }
        }

        System.out.println("Installing");

        new Thread(() -> {
            try {
                Path pzPath = Paths.get(installLocation.getText()).toAbsolutePath();
                if (!Files.exists(pzPath)) {
                    throw new RuntimeException(Utils.BUNDLE.getString("progress.exception.no.launcher.directory"));
                }

                new ServerInstaller(pzPath, gameVersion, loaderVersion, this).install(proxy, createConfig, createScript);

                ServerPostInstallDialog.show(this);
            } catch (Exception e) {
                error(e);
            } finally {
                buttonInstall.setEnabled(true);
            }
        }).start();
    }

    @Override
    public void installCli(ArgumentParser args) throws Exception {
        Path path = Paths.get(args.getOrDefault("dir", () -> Utils.getClientGamePath().toString()));
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Game directory not found at " + path);
        }

        final var os = OperatingSystem.CURRENT.toShortString();

        LoaderVersion loaderVersion = new LoaderVersion(getLoaderVersion(args));
        String gameVersion = getGameVersion(args);
        new ServerInstaller(path, gameVersion, loaderVersion, InstallerProgress.CONSOLE)
            .install(!args.has("manual"), args.has("createConfig"), args.has("createScript"));

        InstallerProgress.CONSOLE.updateProgress(
            new MessageFormat(Utils.BUNDLE.getString("progress.done.start.server." + os)).format(null));
    }

    @Override
    public String cliHelp() {
        return "-dir <install dir> -- (default: current dir) "
            + "-game <version> -- (default: latest) "
            + "-loader <loader version> -- (default: latest)";
    }

    @Override
    public void setupPane1(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
        if (!Desktop.isDesktopSupported() ||
            !Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
        }
    }

    @Override
    public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
        installLocation.setText(Utils.getServerGamePath().toString());

        createScriptCheckbox = new JCheckBox(Utils.BUNDLE.getString("option.create.script"), false);
        addRow(pane, c, null, createScriptCheckbox);
    }
}
