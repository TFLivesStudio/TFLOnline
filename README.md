# TFLOnline

**Cross-server player counts for Minecraft proxies, delivered to PlaceholderAPI — no additional port or Redis required in the default installation.**

> **Development status: 0.1.0-SNAPSHOT / experimental bootstrap.** This is the first functional foundation, **not** the feature-complete V1. Do not deploy to a production network without testing it on a staging proxy and backends first.

TFLOnline is GPL-3.0-licensed and developed by [TFLivesStudio](https://github.com/TFLivesStudio).

## Install (initial snapshot)

1. Put **TFLOnline-Velocity.jar** in your Velocity (or API-compatible fork) `plugins/` folder, **or** **TFLOnline-Bungee.jar** in BungeeCord/Waterfall `plugins/`.
2. Put **TFLOnline-Paper.jar** in the `plugins/` folder of every Paper/Purpur backend where you want PlaceholderAPI values.
3. Install PlaceholderAPI on those backends, then restart the proxy and servers.
4. The proxy generates `plugins/tflonline/tflonline.properties` (actual folder depends on platform). It automatically lists the backend servers registered with the proxy.
5. Edit capacities, aliases and optional server groups in the **proxy** configuration, then **restart the proxy for this initial snapshot**. Live config reload is planned but is not implemented yet.

No database and **no extra listener port** are created. Communication uses Minecraft's existing plugin message channel `tflonline:sync`.

### Available placeholders

| Placeholder | Result |
| --- | --- |
| `%tflonline_total%` | Total players on this proxy |
| `%tflonline_server:lobby:online%` | Connected players on registered backend `lobby` |
| `%tflonline_server:skyblock:online%` | Connected players on `skyblock` |
| `%tflonline_group:survival:online%` | Sum for a **configured group of servers** named `survival` |
| `%tflonline_bar:skyblock%` | Text progress bar of players / configured capacity |
| `%tflonline_sync_status%` | `WAITING`, `CONNECTED` or `STALE` |
| `%tflonline_sync_age%` | Seconds since last valid snapshot (`-1` before first sync) |

A missing server/group/capacity or stale snapshot yields `N/A`, not a misleading zero. `sync_status` and `sync_age` remain queryable when sync is unavailable.

**Group in this snapshot means a group of backends, not a LuckPerms rank.** LuckPerms rank counts will have a separate placeholder namespace when implemented.

### Example proxy configuration

The proxy creates the file automatically, with keys including:

```properties
sync.seconds=2
cache.timeout.seconds=15
bar.segments=20
bar.filled=▌
bar.empty=░
server.lobby.alias=lobby
server.lobby.capacity=100
server.lobby.state=AUTO
server.skyblock.alias=skyblock
server.skyblock.capacity=200
server.skyblock.state=AUTO
group.survival=skyblock,lifesteal
```

- `sync.seconds` affects recurring broadcasts; an immediate snapshot is also requested when a player joins the backend.
- New proxy-registered servers get default entries on the next discovery pass. Existing custom keys are not overwritten.
- The `state` key is **reserved for a later release**; V0.1 does not ping backends or expose status placeholders.
- Set a positive `capacity` to enable `bar:<server>`. `0` means unknown capacity.

## Components

```text
common/    Immutable snapshots, proxy configuration, versioned wire protocol and tests
velocity/  Proxy API adapter for Velocity and compatible forks
bungee/    BungeeCord/Waterfall proxy adapter
paper/     Bukkit/Paper/Folia adapter; built-in PlaceholderAPI expansion
```

The Paper module is a **normal server plugin**. Do **not** copy it into `plugins/PlaceholderAPI/expansions/`; it registers its PAPI expansion internally when PlaceholderAPI is installed.

## Build

Requirements: **JDK 17** and **Gradle 8.14+** installed for local builds. Later Minecraft server versions may require a newer Java runtime even though the project's bytecode target is Java 17.

```bash
gradle clean build
```

JARs are produced under `velocity/build/libs/`, `bungee/build/libs/` and `paper/build/libs/`. GitHub Actions runs `gradle clean build` and uploads the JARs as CI artifacts for commits/PRs.

## Important limitations of the bootstrap

- Only **single-proxy** counting; Redis/multi-proxy federation is **not implemented**.
- No LuckPerms group counts, Geyser/Floodgate platform splits, ping/status checks, per-group ranks, color formatting, admin commands, live reload, localization or release automation **yet**.
- Plugin messages require an existing player connection to the backend. A backend with no players will not receive updates, but a new join requests a fresh snapshot. There is also a periodic broadcast while players are present.
- The backend cache expires after the **configurable** `cache.timeout.seconds` without an update (default: 15 seconds; range: 2–300). The proxy distributes this setting in each snapshot.
- A malicious/untrusted backend plugin may submit false data or disrupt messaging; **secure your backend-to-proxy forwarding setup**. Our proxy adapter handles its own channel and does not forward client-origin messages.
- This snapshot is designed to compile against Paper API 1.20.4, Velocity API 3.3.0 and Bungee API 1.21. Extensive cross-version/Folia/fork compatibility tests are still needed.

See [Roadmap](docs/ROADMAP.md) and [Protocol](docs/PROTOCOL.md) for the design decisions and upcoming milestones.

## License

GPL-3.0 — see [LICENSE](LICENSE).
