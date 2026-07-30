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
import java.awt.datatransfer.StringSelection;
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
        return Utils.BUNDLE.getString("tab.client");
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

                String profileName = new ClientInstaller(pzPath, gameVersion, loaderVersion,
                    this).install(createProfile.isSelected());
                SwingUtilities.invokeLater(() -> {
                    showInstalledMessage(loaderVersion.name, gameVersion,
                        pzPath.resolve(".leaf/mods"));

                    // Copy to clipboard.
                    Toolkit.getDefaultToolkit()
                        .getSystemClipboard()
                        .setContents(
                            new StringSelection(String.format("-pzexeconfig %s.json", profileName)),
                            null);
                });
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

        String gameVersion = getGameVersion(args);
        LoaderVersion loaderVersion = new LoaderVersion(getLoaderVersion(args));

        new ClientInstaller(path, gameVersion, loaderVersion, InstallerProgress.CONSOLE).install(
            !args.has("noprofile"));
    }

    @Override
    public String cliHelp() {
        return "-dir <install dir> " +
               "-pzversion <zomboid version, default latest> " +
               "-loader <loader version, default latest>";
    }

    @Override
    public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
//        addRow(pane, c, null,
//            createProfile = new JCheckBox(Utils.BUNDLE.getString("option.create.config"), false));
        installLocation.setText(Utils.getClientGamePath().toString());
    }

    private void showInstalledMessage(String loaderVersion, String gameVersion,
        Path modsDirectory) {
        JEditorPane pane = new JEditorPane("text/html",
            String.format("<html><body style=\"%s\">%s</body></html>", buildEditorPaneStyle(),
                new MessageFormat(Utils.BUNDLE.getString("prompt.install.successful")).format(
                    new Object[] { loaderVersion, gameVersion, Reference.LEAF_API_URL }
                )));
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
                        throw new UnsupportedOperationException("Failed to open "
                                                                + e.getURL().toString());
                    }
                }
            } catch (Throwable throwable) {
                error(throwable);
            }
        });

        final Image iconImage = Toolkit.getDefaultToolkit()
            .getImage(ClassLoader.getSystemClassLoader().getResource("icon.png"));
        JOptionPane.showMessageDialog(null,
            pane,
            Utils.BUNDLE.getString("prompt.install.successful.title"),
            JOptionPane.INFORMATION_MESSAGE,
            new ImageIcon(iconImage.getScaledInstance(64, 64, Image.SCALE_DEFAULT)));
    }
}
