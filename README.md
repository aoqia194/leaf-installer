<div align="center">

<h1>
    The universal installer for
    <a href="https://pzwiki.net/wiki/Leaf">
        <img src="src/main/resources/icon.png" width="36"> leaf
    </a>
</h1>

![License](https://img.shields.io/github/license/aoqia194/leaf-installer?label=License)
![Gradle version](https://img.shields.io/badge/Gradle-9.7.1-teal?logo=gradle) 
![Build status](https://github.com/aoqia194/leaf-installer/actions/workflows/build.yml/badge.svg?branch=main&label=build) 
![Downloads](https://img.shields.io/github/downloads/aoqia194/leaf-installer/total?label=Downloads)
![Code Size](https://img.shields.io/github/languages/code-size/aoqia194/leaf-installer?label=Code%20Size) 
![Maven status](https://img.shields.io/website?url=https%3A%2F%2Fmaven.aoqia.dev%2F&label=Maven)

</div>

### Requirements

- Java 17 or higher
- **(optional)** [JarFix][1] \
  <small>Useful if you are on Windows and double-clicking the installer JAR doesn't open it.</small>

### Installation

The primary use-case for this project is to install the [leaf-loader][2] and its dependencies. The only thing you need
to do is download the JAR linked in the [latest release][3].

### Usage

1. Run the installer
2. Choose the environment -- client/server
3. Check the game path is set correctly
4. Press install
5. Subscribe to the **[Leaf Loader][4]** mod on the Steam Workshop

***It really is that easy!***

That's all you need to get started! You do not need to enable the leaf loader mod in-game for it to be loaded by the
proxy. You can now subscribe to any Leaf mod on the Workshop, and when you enable it, it will be loaded when you next
start the game.

Using the proxy is the **default recommended** method. You may choose to manually install the loader if you want,
however it will not auto-update the loader or any of its dependencies. It is on you to update where necessary, but it is
safter. Manual installation requires unchecking the `Use proxy` box and by selecting the appropriate game and loader
versions.

The `Create config` box will, when **NOT** using the proxy, create a launcher config for the game to use. It can be used
by copying the argument from the installer and pasting it into your Project Zomboid launch options. The `pzexeconfig`
argument is a game argument, **NOT** a JVM argument. If you do not know the difference, read the
[Startup parameters][4] pzwiki page.

If you still need some help with usage of game arguments in Steam, you should read
[Startup parameters - From the Steam application][5].

### Configuration

**(optional)** If you use leaf regularly either for development (as a developer) or production (as a user),
`LEAF_CLIENT_GAME_PATH` and `LEAF_SERVER_GAME_PATH` environment variables may be set to allow for leaf to automatically
detect the location of your game.

### Development

You can build the project like so:

```shell
./gradlew build
```

For developing your own Leaf mods, you will want to read the [Leaf][7] wiki page which will contain a top-down summary
of the entire leaf project.

### Support

If you need any help whatsoever with Leaf, you can discuss anything leaf-related on Discord through the
official [Project Zomboid Modding Community](https://discord.gg/2Vr6Wyh6Am) Discord using the appropriate channels.

### Special Thanks

- The entire [FabricMC team](https://github.com/FabricMC/)!
- [albion](https://github.com/demiurgeQuantified)
- GigaWatte
- electrisoma
- [SimKDT](https://github.com/SimKDT)

[1]: https://johann.loefflmann.net/en/software/jarfix/index.html
[2]: https://github.com/aoqia194/leaf-loader
[3]: https://github.com/aoqia194/leaf-installer/releases/latest
[4]: https://steamcommunity.com/sharedfiles/filedetails/?id=3776625738
[5]: https://pzwiki.net/wiki/Startup_parameters
[6]: https://pzwiki.net/wiki/Startup_parameters#From_the_Steam_application
[7]: https://pzwiki.net/wiki/Leaf
