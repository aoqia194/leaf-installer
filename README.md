# Leaf Installer

An installer for the Project Zomboid bootstrapper, used to install leaf-loader
(a [fabric-loader](https://github.com/FabricMC/fabric-loader) fork) and its dependencies.

### Usage

- Download the [latest release]((https://github.com/aoqia194/leaf-installer/releases/latest)) JAR
  from the repository releases or alternatively directly from the
  [Maven Central](https://repo.maven.apache.org/maven2/dev/aoqia/leaf/installer)
- Execute the JAR like a normal JAR executable
  (may require [JarFix](https://johann.loefflmann.net/en/software/jarfix/index.html) if on Windows
  and double-click does not work)
- Select the correct game version that is installed and the directory at which the game is installed
- Hit install!
  **Make sure that you read the post-installation message that shows up!**
- Add the launch option `-pzexeconfig leaf-{loaderVersion}-{gameVersion}.json` to your game
  through the Steam Library.
  If you missed the popup message, you can click the **Copy Launch Option** button.

### What game files are modified?

None! All modifications are done by patches at runtime.
In your game directory, there will be 2 new entries.

- A folder named `.leaf` (hidden on Linux by default) which stores libs for leaf to run. This folder can be ignored and deleting it can be safely done if you don't want leaf installed AT ALL
- A file with the name of `leaf-{loaderVersion}-{gameVersion}.json`. This file is a config for the bootstrapper to tell it that we want to run leaf

These files can be safely deleted to **completely uninstall** leaf!

### Special Thanks

The entire [FabricMC team](https://github.com/FabricMC/)!
