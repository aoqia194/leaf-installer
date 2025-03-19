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
package dev.aoqia.installer;

import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.net.URISyntaxException;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Objects;
import java.util.Properties;
import java.util.StringTokenizer;
import java.util.jar.Attributes;
import java.util.jar.JarFile;
import java.util.jar.Manifest;

import dev.aoqia.installer.server.ServerInstaller;
import dev.aoqia.installer.util.InstallerProgress;
import dev.aoqia.installer.util.OperatingSystem;
import dev.aoqia.installer.util.Utils;

public final class ServerLauncher {
    private static final String INSTALL_CONFIG_NAME = "install.properties";
    private static final Path DATA_DIR = Paths.get(".leaf", "server");

    public static void main(String[] args) throws Throwable {
        LaunchData launchData;

        try {
            launchData = initialise();
        } catch (IOException e) {
            throw new RuntimeException("Failed to setup leaf server", e);
        }

        Objects.requireNonNull(launchData, "launchData is null, cannot proceed");

        // Set the game jar path to bypass loader's own lookup
        System.setProperty("leaf.gamePath", launchData.serverPath.toAbsolutePath().toString());

        @SuppressWarnings("resource")
        URLClassLoader launchClassLoader = new URLClassLoader(new URL[] { launchData.launchJar.toUri().toURL() });

        // Use method handle to keep the stacktrace clean
        MethodHandle handle = MethodHandles.publicLookup()
            .findStatic(launchClassLoader.loadClass(launchData.mainClass),
                "main",
                MethodType.methodType(void.class, String[].class));
        handle.invokeExact(args);
    }

    // Validates and downloads/installs the server if required
    private static LaunchData initialise() throws IOException {
        Properties properties = readProperties();

        String customLoaderPath = System.getProperty(";eaf.customLoaderPath"); // intended for testing and development
        LoaderVersion loaderVersion;

        if (customLoaderPath == null) {
            loaderVersion = new LoaderVersion(Objects.requireNonNull(properties.getProperty("leaf-loader-version"),
                "no loader-version specified in " + INSTALL_CONFIG_NAME));
        } else {
            loaderVersion = new LoaderVersion(Paths.get(customLoaderPath));
        }

        String gameVersion = Objects.requireNonNull(properties.getProperty("game-version"),
            "no game-version specified in " + INSTALL_CONFIG_NAME);

        // 1.0 or higher is required
        validateLoaderVersion(loaderVersion);

        Path baseDir = Paths.get(".").toAbsolutePath().normalize();
        Path dataDir = baseDir.resolve(DATA_DIR);

        String customServerPath = System.getProperty("leaf.installer.server.gamePath", null);
        final String ext = (OperatingSystem.CURRENT.equals(OperatingSystem.WINDOWS) ? ".bat" : "sh");
        Path windowsServerScript = customServerPath == null ? dataDir.resolve("StartServer64." + ext)
            : Paths.get(customServerPath).resolve("StartServer64." + ext);

        if (!Files.exists(windowsServerScript)) {
            InstallerProgress.CONSOLE.updateProgress(Utils.BUNDLE.getString("progress.exception.no.server.manifest"));
            throw new RuntimeException("Server install path does not exist.");
        }

        if (Files.exists(windowsServerScript)) {
            final String mainClass = "zombie/network/gameServer";
            if (Files.exists(windowsServerScript.getParent().resolve(mainClass))) {
                return new LaunchData(windowsServerScript, windowsServerScript, mainClass);
            }

            System.err.println("Detected incomplete install, reinstalling");
        }

        Files.createDirectories(dataDir);
        ServerInstaller.install(baseDir, loaderVersion, gameVersion, InstallerProgress.CONSOLE, windowsServerScript);

        String mainClass = readManifest(windowsServerScript, null);

        return new LaunchData(windowsServerScript, windowsServerScript, mainClass);
    }

    private static Properties readProperties() throws IOException {
        Properties properties = new Properties();

        URL config = getConfigFromResources();

        if (config == null) {
            throw new RuntimeException("Jar does not contain unattended install.properties file");
        }

        try (InputStreamReader reader = new InputStreamReader(config.openStream(), StandardCharsets.UTF_8)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new IOException("Failed to read " + INSTALL_CONFIG_NAME, e);
        }

        return properties;
    }

    // Find the mainclass of a jar file
    private static String readManifest(Path path, List<Path> classPathOut) throws IOException {
        try (JarFile jarFile = new JarFile(path.toFile())) {
            Manifest manifest = jarFile.getManifest();
            String mainClass = manifest.getMainAttributes().getValue(Attributes.Name.MAIN_CLASS);

            if (mainClass == null) {
                throw new IOException("Jar does not have a Main-Class attribute");
            }

            if (classPathOut != null) {
                String cp = manifest.getMainAttributes().getValue(Attributes.Name.CLASS_PATH);

                StringTokenizer tokenizer = new StringTokenizer(cp);
                URL baseUrl = path.toUri().toURL();

                while (tokenizer.hasMoreTokens()) {
                    String token = tokenizer.nextToken();
                    URL url = new URL(baseUrl, token);

                    try {
                        classPathOut.add(Paths.get(url.toURI()));
                    } catch (URISyntaxException e) {
                        throw new IOException(String.format("invalid class path entry in %s manifest: %s",
                            path,
                            token));
                    }
                }
            }

            return mainClass;
        }
    }

    private static void validateLoaderVersion(LoaderVersion loaderVersion) {
        if (Utils.compareVersions(loaderVersion.name, "1.0") < 0) {
            throw new UnsupportedOperationException(
                "Leaf loader 1.0 or higher is required for unattended server installs. Please use a newer leaf loader" +
                " version, or the full installer.");
        }
    }

    private static URL getConfigFromResources() {
        return ServerLauncher.class.getClassLoader().getResource(INSTALL_CONFIG_NAME);
    }

    private static class LaunchData {
        final Path serverPath;
        final Path launchJar;
        final String mainClass;

        private LaunchData(Path serverPath, Path launchJar, String mainClass) {
            this.serverPath = Objects.requireNonNull(serverPath, "serverPath");
            this.launchJar = Objects.requireNonNull(launchJar, "launchJar");
            this.mainClass = Objects.requireNonNull(mainClass, "mainClass");
        }
    }
}
