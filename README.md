# Leaf Installer

An installer for the Project Zomboid bootstrapper, used to install leaf-loader
(a [fabric-loader](https://github.com/FabricMC/fabric-loader) fork) and its dependencies.

### Usage

- Download the [latest release]((https://github.com/aoqia194/leaf-installer/releases/latest)) JAR
  from the repository releases or alternatively directly from the
  [Maven Central](https://maven.aoqia.dev/releases/dev/aoqia/leaf/installer)
- Execute the JAR like a normal JAR executable
  (may require [JarFix](https://johann.loefflmann.net/en/software/jarfix/index.html) if on Windows
  and double-click does not work)
- Select the correct game version that is installed and the directory at which the game is installed
- Hit install!

### What game files are modified?

None! All modifications are done by patches at runtime.
In your game directory, there will be a new directory named `.leaf` (hidden on Linux by default)
which stores libraries for leaf to be able to run.

These files can be safely deleted to **completely uninstall** leaf!
There is also a `.leaf` folder created in your cachedir which will store leaf configuration state,
but does not make leaf itself work.

### Special Thanks

The entire [FabricMC team](https://github.com/FabricMC/)!
