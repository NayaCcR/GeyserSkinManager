# GeyserSkinManager

### Primarily the server-side companion for the [BedrockSkinUtility](https://github.com/Camotoy/BedrockSkinUtility) Fabric mod.

#### Skin visibility for non-modded players can be viewed without Floodgate installed, Or by changing the config option "force-show-skins:" from false to true.

Supported platforms:

- Velocity 3.2 through 4.1
- Paper/Purpur 1.20.x through 26.2

The same plugin JAR is used throughout each range. Run the Java version required by
the proxy or Minecraft server itself (for example, Velocity 4.1 requires Java 21 and
Paper/Purpur 26.2 requires Java 25).

Known caveats:

- The backend plugin now targets the Paper API and does not support a plain Spigot server.
- If using without BungeeCord, this plugin requires Geyser-Spigot installed on the server; Floodgate alone will not work.
- If using with BungeeCord or Velocity, install the matching Geyser proxy plugin on the proxy.
- Persona skins are not supported

How to set up BungeeCord/Velocity:

- Install GeyserSkinManager-BungeeCord on BungeeCord, or GeyserSkinManager-Velocity on Velocity.
- Install GeyserSkinManager-Spigot on each Paper/Purpur backend where skins should be applied. Geyser-Spigot should not be installed on these backend servers.
- Done.

Camotoy's Discord server: https://discord.gg/jNNC4CZtsN
