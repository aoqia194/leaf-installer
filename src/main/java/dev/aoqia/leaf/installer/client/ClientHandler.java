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
package dev.aoqia.leaf.installer.client;

import java.awt.*;
import java.io.FileNotFoundException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.MessageFormat;

import dev.aoqia.leaf.installer.Handler;
import dev.aoqia.leaf.installer.InstallerGui;
import dev.aoqia.leaf.installer.LoaderVersion;
import dev.aoqia.leaf.installer.util.*;
import javax.swing.*;
import javax.swing.event.HyperlinkEvent;

public class ClientHandler extends Handler {
    private JCheckBox createProfile;

    @Override
    public String name() {
        return "Client";
    }

    @Override
    public void install() {
        String gameVersion = (String) gameVersionComboBox.getSelectedItem();
        LoaderVersion loaderVersion = queryLoaderVersion();
        if (loaderVersion == null) {
            return;
        }

        System.out.println("Installing");

        new Thread(() -> {
            try {
                updateProgress(new MessageFormat(
                    Utils.BUNDLE.getString("progress.installing")).format(new Object[] {
                    loaderVersion.name }));

                Path pzPath = Paths.get(installLocation.getText());
                if (!Files.exists(pzPath)) {
                    throw new RuntimeException(Utils.BUNDLE.getString(
                        "progress.exception.no.launcher.directory"));
                }

                String profileName = ClientInstaller.install(pzPath, gameVersion, loaderVersion,
                    createProfile.isSelected(), this);
                SwingUtilities.invokeLater(() -> showInstalledMessage(loaderVersion.name,
                    gameVersion, pzPath.resolve(".leaf/mods")));
            } catch (Exception e) {
                error(e);
            } finally {
                buttonInstall.setEnabled(true);
            }
        }).start();
    }

    @Override
    public void installCli(ArgumentParser args) throws Exception {
        Path path = Paths.get(args.getOrDefault("dir", () -> {
            return Utils.getClientInstallPath().toString();
        }));
        if (!Files.exists(path)) {
            throw new FileNotFoundException("Game directory not found at " + path);
        }

        String gameVersion = getGameVersion(args);
        LoaderVersion loaderVersion = new LoaderVersion(getLoaderVersion(args));

        String profileName = ClientInstaller.install(path, gameVersion, loaderVersion,
            args.has("noprofile"), InstallerProgress.CONSOLE);
    }

    @Override
    public String cliHelp() {
        return "-dir <install dir> " +
               "-pzversion <zomboid version, default latest> " +
               "-loader <loader version, default latest>";
    }

    @Override
    public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
        addRow(pane, c, null,
            createProfile = new JCheckBox(Utils.BUNDLE.getString("option.create.profile"), true));
        installLocation.setText(Utils.getClientInstallPath().toString());
    }

    private void showInstalledMessage(String loaderVersion, String gameVersion,
        Path modsDirectory) {
        JEditorPane pane = new JEditorPane("text/html",
            "<html><body style=\"" + buildEditorPaneStyle() + "\">" +
            new MessageFormat(Utils.BUNDLE.getString("prompt.install.successful")).format(
                new Object[] { loaderVersion, gameVersion, Reference.LEAF_API_URL }
            ) + "</body></html>");
        pane.setBackground(new Color(0, 0, 0, 0));
        pane.setEditable(false);
        pane.setCaret(new NoopCaret());

        pane.addHyperlinkListener(e -> {
            try {
                if (e.getEventType() == HyperlinkEvent.EventType.ACTIVATED) {
                    if (e.getDescription().equals("leaf://mods")) {
                        Desktop.getDesktop().open(modsDirectory.toRealPath().toFile());
                    } else if (Desktop.isDesktopSupported() &&
                               Desktop.getDesktop().isSupported(Desktop.Action.BROWSE)) {
                        Desktop.getDesktop().browse(e.getURL().toURI());
                    } else {
                        throw new UnsupportedOperationException(
                            "Failed to open " + e.getURL().toString());
                    }
                }
            } catch (Throwable throwable) {
                error(throwable);
            }
        });

        final Image iconImage = Toolkit.getDefaultToolkit()
            .getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
        JOptionPane.showMessageDialog(
            null,
            pane,
            Utils.BUNDLE.getString("prompt.install.successful.title"),
            JOptionPane.INFORMATION_MESSAGE,
            new ImageIcon(iconImage.getScaledInstance(64, 64, Image.SCALE_DEFAULT))
        );
    }
}
