# TFLOnline — proxy configuration guide

The **proxy** is the only authoritative configuration location. Paper does not need a config.yml.

TFLOnline creates a fully commented `config.yml` and discovers registered servers automatically. Read the comments inside the generated file for concrete examples.

## Sections

| Section | Implemented settings | Purpose |
|---|---|---|
| `general` | `language`, `debug` | Spanish/English administrative messages; logging setting |
| `synchronization` | `transport`, `broadcast-interval-seconds`, `cache-timeout-seconds`, `broadcast-on-events`, `event-debounce-millis` | Plugin Messaging delivery/caching |
| `discovery` | `auto-register`, `preserve-missing` | Automatically append registered servers; never remove customized server entries |
| `bars.default` | `segments`, `filled`, `empty` | Global bar renderer settings |
| `groups` | Mappings from group name to backend IDs | Group-of-backends counters, not LuckPerms ranks |
| `servers` | `alias`, `capacity`, `state` | Discovered backend definitions and admin metadata |

### Example

```yaml
config-version: 1

general:
  language: es
  debug: false

synchronization:
  transport: plugin-messaging
  broadcast-interval-seconds: 3
  cache-timeout-seconds: 15
  broadcast-on-events: true
  event-debounce-millis: 150

discovery:
  auto-register: true
  preserve-missing: true

bars:
  default:
    segments: 20
    filled: "▌"
    empty: "░"

groups:
  survival:
    - skyblock
    - lifesteal

servers:
  skyblock:
    alias: skyblock
    capacity: 100
    state: AUTO
  lifesteal:
    alias: lifesteal
    capacity: 75
    state: AUTO
```

## Constraints and validation

- `config-version`: exactly `1`.
- `general.language`: `es` or `en`.
- `synchronization.transport`: only `plugin-messaging` in the current snapshot.
- `broadcast-interval-seconds`: 1–60, `cache-timeout-seconds`: 2–300 and strictly greater than the broadcast interval.
- `event-debounce-millis`: 0–2000.
- `bars.default.segments`: 1–100.
- `servers.*.capacity`: nonnegative integer; `0` disables occupancy-bar output.
- All aliases must be unique, without whitespace.
- Backend names must match names registered in Velocity/BungeeCord.
- `servers.*.state` is **administrative metadata** available as `%tflonline_server:<id>:state%`, not a live reachability test.
- `discovery.preserve-missing` must remain `true`; `false` is rejected to prevent accidental loss of customized backends.
- Invalid reloads preserve the previously loaded valid settings in memory.

The scanner appends newly discovered backends without rebuilding the entire YAML file, so the original section headings and user comments remain intact.

## Migration

If `tflonline.properties` exists and there is no `config.yml`, TFLOnline imports the supported interval, cache time, bars, aliases, capacities, group definitions and states, then leaves the original properties file in place for rollback.

## Troubleshooting

| Symptom | Check |
|---|---|
| `WAITING` | A player must have joined that Paper server through the configured proxy |
| `STALE` | Verify the proxy plugin, `tflonline:sync` and backend forwarding |
| `N/A` | Unknown backend ID, disabled bar capacity, or stale data |
| `0` players | The proxy has a valid snapshot reporting zero |
| Missing backend in config | Confirm its name is registered in the proxy and discovery is enabled |
| Config reload rejected | YAML spacing, duplicate keys, supported values and error log |
| No Java classes inside ZIP download | Extract the separate Actions artifact to obtain the .jar |

Security: protect backend listener ports from direct public connections; only trusted proxy connections should reach backend plugin messaging.
