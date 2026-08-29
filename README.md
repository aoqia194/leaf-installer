<div align="center">

## **The universal installer for [<img src="src/main/resources/icon.png" width="18" height="auto"> leaf][1]**

![Gradle Version](https://img.shields.io/badge/Gradle-9.5.0-teal?logo=gradle)
![License](https://img.shields.io/badge/License-Apache%202.0-orange)
![Build Status](https://github.com/aoqia194/leaf-installer/actions/workflows/build.yml/badge.svg?branch=main)

</div>

### Requirements

- Java 17 or higher
- **(optional)** [JarFix][2] \
  <small>Useful if you are on Windows and double-clicking the installer JAR doesn't open it.</small>

### Installation

The only thing you need to do is download the JAR linked in the [latest release][3].

### Usage

1. Run the installer
2. Choose the environment -- client/server
3. Check the game path is set correctly
4. Press install

***It really is that easy!***

Using the proxy is the **default recommended** method. You may choose to manually install the loader if you want,
however it will not auto-update the loader or any of its dependencies. It is on you to update where necessary, but it is
safter. Manual installation requires unchecking the "Use proxy" box and by selecting the appropriate game and loader
versions.

The `Create config` box will, when **NOT** using the proxy, create a launcher config for the game to use. It can be used
by copying the argument from the installer and pasting it into your Project Zomboid launch options. The `pzexeconfig`
argument is a game argument, **NOT** a JVM argument. If you do not know the difference, read the
[Startup parameters][4] pzwiki page.

If you still need some help with usage of game arguments in Steam, you should read
[Startup parameters - From the Steam application][5].

### Configuration

**(optional)** If you use leaf regularly either for development (as a developer) or production (as a user),
`LEAF_CLIENT_GAME_PATH` and `LEAF_SERVER_GAME_PATH` environment variables may want to be set to allow for leaf to
automatically detect the location of your game.

### Development

You can build the project like so:

```shell
./gradlew build
```

An installer for the Project Zomboid bootstrapper, used to install leaf-loader
(a [fabric-loader](https://github.com/FabricMC/fabric-loader) fork) and its dependencies.

### Special Thanks

The entire [FabricMC team](https://github.com/FabricMC/)!

[1]: https://pzwiki.net/wiki/Leaf
[2]: https://johann.loefflmann.net/en/software/jarfix/index.html
[3]: https://github.com/aoqia194/leaf-installer/releases/latest
[4]: https://pzwiki.net/wiki/Startup_parameters
[5]: https://pzwiki.net/wiki/Startup_parameters#From_the_Steam_application
