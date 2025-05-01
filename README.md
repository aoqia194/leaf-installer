# Leaf Installer

An installer for the Project Zomboid bootstrapper, used to install leaf-loader (
a [fabric-loader](https://github.com/FabricMC/fabric-loader) fork) and its dependencies.

### Usage

- Download the [latest release]((https://github.com/aoqia194/leaf-installer/releases/latest)) JAR
  from the repository releases or alternatively directly from
  the [Maven Central](https://repo.maven.apache.org/maven2/dev/aoqia/leaf/installer)
- Execute the JAR like a normal JAR executable (may
  require [JarFix](https://johann.loefflmann.net/en/software/jarfix/index.html) if on Windows and
  double-click does not work)
- Select the correct game version that is installed and the directory at which the game is installed
- Hit install!
- Add the launch option `-pzexeconfig loader-{loaderVersion}-{gameVersion}` to your game through the
  Steam Library

Make sure that you read the post-installation message that shows up!

### Special Thanks

The entire [FabricMC team](https://github.com/FabricMC/)!
