# TFLOnline

TFLOnline is a Minecraft network player-counter project by **tflives**, licensed under **GPL-3.0**.

The initial installation uses Minecraft's existing Plugin Messaging connection. **No extra listener port, database or Redis is required.**

> **Status: 0.2.0-SNAPSHOT (public testing).** Compilation and unit tests do not replace live tests on Velocity, BungeeCord, Paper, forks or Folia. Features marked as planned are not yet implemented.

## Downloads

- **GitHub Actions:** open the most recent successful [Build & Test](https://github.com/TFLivesStudio/TFLOnline/actions/workflows/build.yml). The artifacts are named `TFLOnline-Velocity-JAR`, `TFLOnline-Bungee-JAR`, and `TFLOnline-Paper-JAR`. GitHub downloads Actions artifacts as **ZIP archives**. Extract each ZIP to obtain its executable `.jar`.
- **GitHub Releases:** every successful push to `main` publishes a development **prerelease with the three individual JAR files**. A tag like `v0.2.0` runs CI and, if all tests succeed, publishes **three individual executable JAR files** plus `SHA256SUMS.txt` on the [Releases page](https://github.com/TFLivesStudio/TFLOnline/releases). Regular feature-branch CI artifacts are ZIP wrappers; development Releases expose the JAR files directly.

### Which JAR goes where?

| Platform | Install |
|---|---|
| Velocity and compatible forks | `TFLOnline-Velocity-<version>.jar` into proxy `plugins/` |
| BungeeCord/Waterfall | `TFLOnline-Bungee-<version>.jar` into proxy `plugins/` |
| Paper/Purpur/Folia | `TFLOnline-Paper-<version>.jar` into each backend `plugins/` |

The **Paper component is a normal plugin**. It registers its PlaceholderAPI expansion automatically when PlaceholderAPI is installed. **Do not** put the Paper JAR in `plugins/PlaceholderAPI/expansions/`.

Use **one proxy adapter**, Velocity or Bungee, never both on the same proxy. Backends must be protected from direct public access because plugin messages presume the proxy is trusted.

## Installation

1. Install the correct proxy JAR and the Paper JAR on each backend where placeholders are needed.
2. Install PlaceholderAPI in those Paper backends.
3. Restart the proxy and backends.
4. TFLOnline creates `plugins/tflonline/config.yml` in Velocity's data directory or `plugins/TFLOnline/config.yml` under BungeeCord (folder casing may depend on your proxy).
5. Registered backends are automatically appended to `servers:` without overwriting existing settings.
6. Edit the thoroughly documented `config.yml` **on the proxy only**, then run `/tflonline reload`.

Existing `tflonline.properties` installations are migrated automatically when `config.yml` does not exist. The old file is preserved.

### Available placeholders

| Placeholder | Purpose |
|---|---|
| `%tflonline_total%` | Players connected to the proxy |
| `%tflonline_server:skyblock:online%` | Players connected to a backend |
| `%tflonline_group:survival:online%` | Sum of the configured group of **backends** |
| `%tflonline_bar:skyblock%` | Occupancy bar (requires a positive capacity) |
| `%tflonline_server:skyblock:capacity%` | Configured capacity (0 = unknown) |
| `%tflonline_server:skyblock:state%` | Administrator-configured state, not a live reachability result |
| `%tflonline_sync_status%` | `WAITING`, `CONNECTED`, `STALE` |
| `%tflonline_sync_age%` | Age in seconds of the last snapshot |

`%tflonline_group:...%` does **not** refer to LuckPerms ranks. Rank counts, Java/Bedrock split, health/ping and Redis federation are still planned and are not advertised as working.

Missing or stale data returns `N/A`. Zero always means a **valid measured count of zero**.

### Proxy commands

Requires `tflonline.admin` (or console access):

- `/tflonline status`
- `/tflonline servers`
- `/tflonline groups`
- `/tflonline sync`
- `/tflonline reload`
- `/tflonline debug`

Messages support Spanish and English using `general.language`. Placeholder values are numbers, statuses or bar characters rather than styled chat messages.

## Configuration

See [config.yml reference](docs/CONFIGURATION.md) and the complete heavily commented [default template](common/src/main/resources/tflonline-config.yml).

### Communication limits

Plugin Messaging only traverses existing player connections. Empty backends cannot receive updates; the Paper plugin requests synchronization after a player joins, and the proxy periodically broadcasts to occupied backends. Presentation may take additional seconds due to the refresh interval of your scoreboard, TAB, DeluxeMenus or hologram plugin.

There is no extra listener, TCP service or Redis client in the current snapshot. Multi-proxy aggregation, protocol encryption, LuckPerms counting and Bedrock/Java splits are not present yet.

## Build

Gradle 8.14.3 and JDK 17 are used in CI. Target bytecode is Java 17. Newer Minecraft releases can require newer Java runtimes.

```bash
gradle clean build
```

The executable, shaded JARs are built in `velocity/build/libs/`, `bungee/build/libs/` and `paper/build/libs/`. The build verifies shaded SnakeYAML and plugin metadata before uploading the separate JAR artifacts.

A tag of the form `vX.Y.Z` starts release creation after successful verification; the tag version should match the version used by the build before publication.

## Design and roadmap

- [Protocol](docs/PROTOCOL.md)
- [Roadmap](docs/ROADMAP.md)
- [Configuration](docs/CONFIGURATION.md)

Copyright tflives and contributors. Distributed under [GPL-3.0](LICENSE).
