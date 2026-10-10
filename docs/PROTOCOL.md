# TFLOnline wire protocol v1 (bootstrap)

Transport channel: `tflonline:sync`, Minecraft plugin messaging.

## Security boundary

Proxy receives messages over this registered channel. It **consumes every packet** on this channel, checks that the **source connection is a backend server**, then accepts only a strict REQUEST frame. It must never forward client-origin custom payloads into a backend.

Paper accepts validated SNAPSHOT frames into an immutable cache. Plugin messaging is trusted only within a properly secured Minecraft proxy/backend deployment. There is no independent cryptographic authentication in protocol v1.

## Frame layout

All integers use Java DataInput/DataOutputStream big-endian encoding. Strings use `writeUTF` (modified UTF-8, two-byte length prefix). Maximum serialized frame: 30,000 bytes.

Shared header:
- `i32 magic` = `0x54464c4f` ("TFLO")
- `u8 version` = `1`
- `u8 type` = `1` REQUEST or `2` SNAPSHOT

REQUEST contains only the header (6 bytes).

SNAPSHOT fields after header:

```text
i64 revision
i32 total
i32 barSegments
UTF filledCharacter
UTF emptyCharacter
i32 cacheTimeoutSeconds
u16 serverCount
repeat serverCount:
  UTF proxyServerId
  UTF alias
  i32 players
  i32 capacity
  UTF stateOverride
u16 groupCount
repeat groupCount:
  UTF groupName
  u16 memberCount
  repeat memberCount: UTF serverIdOrAlias
```

Bounds: at most 500 servers, at most 200 groups and 500 entries per group; snapshot validations reject negative counts, invalid segment count, duplicates, truncated frames and trailing bytes. Unknown protocol versions fail closed.

The `revision` is only monotonic within one running proxy process. It is not a permanent distributed sequence ID and must not be used as one when implementing Redis federation.

## Delivery

- On Paper player join: backend sends REQUEST over that player's Minecraft connection.
- On proxy connection transitions and periodic ticks: proxy broadcasts one snapshot to each currently occupied backend.
- When backend is empty, it cannot participate in plugin messages. At next join, a REQUEST/periodic broadcast refreshes its cache.
- Paper records its own monotonic local arrival timestamp. PAPI returns `N/A` if the snapshot is missing or stale.

## Future evolution

Changes to field layout require a new protocol version and explicit migration. Redis payloads will use separate instance/session identity and must not simply concatenate independent proxy totals.
