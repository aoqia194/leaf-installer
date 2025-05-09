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
package dev.aoqia.leaf.installer;

import javax.swing.*;
import javax.swing.border.EmptyBorder;
import javax.swing.filechooser.FileNameExtensionFilter;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Locale;

import dev.aoqia.leaf.installer.util.ArgumentParser;
import dev.aoqia.leaf.installer.util.InstallerProgress;
import dev.aoqia.leaf.installer.util.MetaHandler;
import dev.aoqia.leaf.installer.util.Utils;

public abstract class Handler implements InstallerProgress {
    protected static final int HORIZONTAL_SPACING = 4;
    protected static final int VERTICAL_SPACING = 6;

    private static final String SELECT_CUSTOM_ITEM = "(select custom)";

    public JButton buttonInstall;

    public JComboBox<String> gameVersionComboBox;
    public JTextField installLocation;
    public JButton selectFolderButton;
    public JLabel statusLabel;
    public JCheckBox unstableCheckbox;
    private JComboBox<String> loaderVersionComboBox;
    private JPanel pane;

    protected static Component createSpacer() {
        return Box.createRigidArea(new Dimension(4, 0));
    }

    public abstract String name();

    public abstract void install();

    public abstract void installCli(ArgumentParser args) throws Exception;

    public abstract String cliHelp();

    //this isnt great, but works
    public void setupPane1(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
    }

    public void setupPane2(JPanel pane, GridBagConstraints c, InstallerGui installerGui) {
    }

    public JPanel makePanel(InstallerGui installerGui) {
        pane = new JPanel(new GridBagLayout());
        pane.setBorder(new EmptyBorder(4, 4, 4, 4));

        GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(VERTICAL_SPACING,
            HORIZONTAL_SPACING,
            VERTICAL_SPACING,
            HORIZONTAL_SPACING);
        c.gridx = c.gridy = 0;

        setupPane1(pane, c, installerGui);

        addRow(pane, c, "prompt.game.version",
            gameVersionComboBox = new JComboBox<>(),
            createSpacer(),
            unstableCheckbox = new JCheckBox(Utils.BUNDLE.getString("option.show" +
                                                                    ".unstable")));
        unstableCheckbox.setSelected(false);
        unstableCheckbox.addActionListener(e -> {
            if (Main.GAME_VERSION_META.isComplete()) {
                updateGameVersions();
            }
        });

        Main.GAME_VERSION_META.onComplete(versions -> {
            updateGameVersions();
        });

        addRow(pane, c, "prompt.loader.version",
            loaderVersionComboBox = new JComboBox<>());

        addRow(pane, c, "prompt.select.location", installLocation = new JTextField(20),
            selectFolderButton = new JButton());
        selectFolderButton.setText("...");
        selectFolderButton.setPreferredSize(
            new Dimension(installLocation.getPreferredSize().height,
                installLocation.getPreferredSize().height));
        selectFolderButton.addActionListener(
            e -> InstallerGui.selectInstallLocation(() -> installLocation.getText(),
                s -> installLocation.setText(s)));

        setupPane2(pane, c, installerGui);

        addRow(pane, c, null,
            statusLabel = new JLabel());
        statusLabel.setText(Utils.BUNDLE.getString("prompt.loading.versions"));

        addLastRow(pane, c, null,
            buttonInstall = new JButton(Utils.BUNDLE.getString("prompt.install")));
        buttonInstall.addActionListener(e -> {
            buttonInstall.setEnabled(false);
            install();
        });

        Main.LOADER_META.onComplete(versions -> {
            int stableIndex = -1;
            for (int i = 0; i < versions.size(); ++i) {
                final var version = versions.get(i);

                loaderVersionComboBox.addItem(version.id());
                if (!version.isUnstable()) {
                    stableIndex = i;
                }
            }

            loaderVersionComboBox.addItem(SELECT_CUSTOM_ITEM);

            //If no stable versions are found, default to the latest version
            if (stableIndex == -1) {
                stableIndex = 0;
            }

            loaderVersionComboBox.setSelectedIndex(stableIndex);
            statusLabel.setText(Utils.BUNDLE.getString("prompt.ready.install"));
        });

        return pane;
    }

    private void updateGameVersions() {
        gameVersionComboBox.removeAllItems();

        for (MetaHandler.ComponentVersion version : Main.GAME_VERSION_META.getVersions()) {
            if (!unstableCheckbox.isSelected() && version.isUnstable()) {
                continue;
            }

            gameVersionComboBox.addItem(version.id());
        }

        gameVersionComboBox.setSelectedIndex(0);

        InstallerGui.instance.updateSize(false);
    }

    protected LoaderVersion queryLoaderVersion() {
        String ret = (String) loaderVersionComboBox.getSelectedItem();
        assert ret != null;

        if (!ret.equals(SELECT_CUSTOM_ITEM)) {
            return new LoaderVersion(ret);
        } else {
            // ask user for loader jar

            JFileChooser chooser = new JFileChooser();
            chooser.setCurrentDirectory(new File("."));
            chooser.setDialogTitle("Select Leaf Loader JAR");
            chooser.setFileFilter(new FileNameExtensionFilter("Java Archive", "jar"));
            chooser.setAcceptAllFileFilterUsed(false);

            if (chooser.showOpenDialog(null) != JFileChooser.APPROVE_OPTION) {
                return null;
            }

            File file = chooser.getSelectedFile();

            // determine loader version from fabric.mod.json

            try {
                return new LoaderVersion(file.toPath());
            } catch (IOException e) {
                error(e);
                return null;
            }
        }
    }

    @Override
    public void updateProgress(String text) {
        statusLabel.setText(text);
        statusLabel.setForeground(UIManager.getColor("Label.foreground"));
    }

    @Override
    public void error(Throwable throwable) {
        StringWriter sw = new StringWriter(800);

        try (PrintWriter pw = new PrintWriter(sw)) {
            throwable.printStackTrace(pw);
        }

        String st = sw.toString().trim();
        System.err.println(st);

        String html = String.format("<html><body style=\"%s\">%s</body></html>",
            buildEditorPaneStyle(),
            st.replace(System.lineSeparator(), "<br>").replace("\t", "&ensp;"));
        JEditorPane textPane = new JEditorPane("text/html", html);
        textPane.setEditable(false);

        statusLabel.setText(throwable.getLocalizedMessage());
        statusLabel.setForeground(Color.RED);

        JOptionPane.showMessageDialog(pane,
            textPane,
            Utils.BUNDLE.getString("prompt.exception.occurrence"),
            JOptionPane.ERROR_MESSAGE);
    }

    protected String buildEditorPaneStyle() {
        JLabel label = new JLabel();
        Font font = label.getFont();
        Color color = label.getBackground();
        return String.format(Locale.ENGLISH,
            "font-family:%s;font-weight:%s;font-size:%dpt;background-color: rgb(%d,%d," +
            "%d);",
            font.getFamily(),
            (font.isBold() ? "bold" : "normal"),
            font.getSize(),
            color.getRed(),
            color.getGreen(),
            color.getBlue()
        );
    }

    protected void addRow(Container parent,
        GridBagConstraints c,
        String label,
        Component... components) {
        addRow(parent, c, false, label, components);
    }

    protected void addLastRow(Container parent,
        GridBagConstraints c,
        String label,
        Component... components) {
        addRow(parent, c, true, label, components);
    }

    private void addRow(Container parent,
        GridBagConstraints c,
        boolean last,
        String label,
        Component... components) {
        if (label != null) {
            c.gridwidth = 1;
            c.anchor = GridBagConstraints.LINE_END;
            c.fill = GridBagConstraints.NONE;
            c.weightx = 0;
            parent.add(new JLabel(Utils.BUNDLE.getString(label)), c);
            c.gridx++;
            c.anchor = GridBagConstraints.LINE_START;
            c.fill = GridBagConstraints.HORIZONTAL;
        } else {
            c.gridwidth = 2;
            if (last) {
                c.weighty = 1;
            }
            c.anchor = last ? GridBagConstraints.PAGE_START : GridBagConstraints.CENTER;
            c.fill = GridBagConstraints.NONE;
        }

        c.weightx = 1;

        JPanel panel = new JPanel();
        panel.setLayout(new BoxLayout(panel, BoxLayout.X_AXIS));

        for (Component comp : components) {
            panel.add(comp);
        }

        parent.add(panel, c);

        c.gridy++;
        c.gridx = 0;
    }

    protected String getGameVersion(ArgumentParser args) {
        return args.getOrDefault("pzversion", () -> {
            System.out.println("Using latest game version");

            return Main.GAME_VERSION_META.getLatestVersion(args.has("unstable")).id();
        });
    }

    protected String getLoaderVersion(ArgumentParser args) {
        return args.getOrDefault("loader", () -> {
            System.out.println("Using latest loader version");

            return Main.LOADER_META.getLatestVersion(false).id();
        });
    }
}
