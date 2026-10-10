# TFLOnline — agreed scope and milestones

This document records the intended public V1 and clearly separates it from implemented work.

## Product decisions

- Public plugin, not locked to TFL NETWORK; name TFLOnline is provisional.
- Proxy adapters: Velocity (compatible forks such as VelocityCTD/VeloFlame PRO) and BungeeCord (including Waterfall).
- Backend: Paper/Purpur/Folia, with built-in PlaceholderAPI expansion, targeting Minecraft 1.20+ where practical.
- Default transport: Minecraft plugin messaging, **zero additional listener ports**.
- Redis: optional transport for independent synchronization/multiple proxies; never mandatory for a single proxy.
- Configuration and server auto-discovery: on proxy; preserve customized settings when new servers are added.
- Languages: Spanish and English, configurable messages supporting legacy, &#RRGGBB and MiniMessage as appropriate to each output consumer.
- License: GPL-3.0. Gradle monorepo, GitHub Actions build and release automation.
- Operational philosophy: non-blocking I/O, bounded messages, bounded caches, explicit stale state, no network operations inside placeholder evaluation.

## Implemented in 0.1.0 bootstrap

- Shared immutable snapshots and versioned protocol with validation.
- Automatic discovery of registered backend names on each proxy.
- Velocity/Bungee proxy adapters.
- Paper plugin with PAPI integration and atomic cache.
- Online counts (total/backend), configurable groups of servers and occupancy bars.
- Periodic proxy broadcasts and backend join-time sync requests.
- Defensive packet handling and unit tests.
- Initial CI compilation/test workflow.

## Work remaining before stable V1

### Phase 1 — harden base

- Compile and test both proxy adapters across supported API versions and fork families.
- Folia thread-affinity and backend join/reconnect integration tests.
- Proxy hot reload and commands: `/tflonline status|servers|groups|sync|reload|debug`.
- Localized command messages, proxy permissions, safe file writes and configuration migration.
- Configuration schema and aliases with explicit validation.
- Debounced event-driven broadcasts and observable sync metrics.
- Connection loss, malformed packet, large network and switching-server soak tests.

### Phase 2 — rank/platform data

- Paper LuckPerms provider for effective primary and inherited groups, context-aware.
- Per-backend and global *online* rank counts, no double counting when switching servers.
- Incremental membership refresh for rank/permission changes.
- Floodgate/Geyser detection with Java, Bedrock and UNKNOWN counts; invariant total = all platform categories.
- Group and platform data protocol compatibility/versioning.

### Phase 3 — health and appearance

- Asynchronous backend ping with explicit technical reachability vs administrator override (`AUTO`, `MAINTENANCE`, `STARTING`, etc.).
- Latency and configurable capacity/occupancy bars.
- Localization and formatting (Legacy/Hex/MiniMessage) with per-consumer compatibility considerations.
- Stale value policy configurable centrally, automatic recovery, health diagnostics.

### Phase 4 — distributed mode and publishing

- Optional Redis pub/sub with snapshot replay, unique proxy IDs, lease expiry and player deduplication.
- Distributed global totals with multi-proxy failover tests.
- Signed-tag GitHub Releases for three JARs, checksums and automated changelog.
- Complete English/Spanish documentation, test matrix and performance benchmarks.

## Defined semantics

- Total = connected unique players as reported by the relevant proxy, not sum of server values.
- Server player count = connections currently assigned to that registered backend.
- **Server group** = sum across unique selected registered backends.
- **LuckPerms rank** (planned) = effective primary group by default; inherited membership optional. A player's current backend context is authoritative for contextual rank counts.
- A missing/stale value must never silently masquerade as zero.
- Admin state overrides must not be confused with technical liveness.
- No claim of real-time zero-latency GUI updates: downstream display plugins have their own refresh intervals.
