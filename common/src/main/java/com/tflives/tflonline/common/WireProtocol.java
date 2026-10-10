package com.tflives.tflonline.common;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class WireProtocol {
    public static final String CHANNEL = "tflonline:sync";
    private static final int MAGIC = 0x54464C4F;
    private static final int VERSION = 1;
    private static final int REQUEST = 1;
    private static final int SNAPSHOT = 2;
    public static final int MAX_BYTES = 30_000;
    private static final int MAX_SERVERS = 500;
    private static final int MAX_GROUPS = 200;
    private WireProtocol() {}

    public static byte[] request() {
        return new byte[] {0x54, 0x46, 0x4c, 0x4f, 1, 1};
    }

    public static boolean isRequest(byte[] bytes) {
        return bytes != null && bytes.length == 6 && header(bytes, REQUEST);
    }

    private static boolean header(byte[] bytes, int type) {
        if (bytes.length < 6) return false;
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            return in.readInt() == MAGIC && in.readUnsignedByte() == VERSION && in.readUnsignedByte() == type;
        } catch (IOException ignored) {
            return false;
        }
    }

    public static byte[] encode(NetworkSnapshot snap) throws IOException {
        if (snap.servers().size() > MAX_SERVERS || snap.groups().size() > MAX_GROUPS) {
            throw new IOException("Too many servers or groups");
        }
        var buffer = new ByteArrayOutputStream();
        try (var out = new DataOutputStream(buffer)) {
            out.writeInt(MAGIC);
            out.writeByte(VERSION);
            out.writeByte(SNAPSHOT);
            out.writeLong(snap.revision());
            out.writeInt(snap.total());
            out.writeInt(snap.barSegments());
            out.writeUTF(snap.barFilled());
            out.writeUTF(snap.barEmpty());
            out.writeInt(snap.cacheTimeoutSeconds());
            out.writeShort(snap.servers().size());
            for (var entry : snap.servers().entrySet()) {
                out.writeUTF(entry.getKey());
                out.writeUTF(entry.getValue().alias());
                out.writeInt(entry.getValue().players());
                out.writeInt(entry.getValue().capacity());
                out.writeUTF(entry.getValue().state());
            }
            out.writeShort(snap.groups().size());
            for (var entry : snap.groups().entrySet()) {
                if (entry.getValue().size() > MAX_SERVERS) throw new IOException("Too many group members");
                out.writeUTF(entry.getKey());
                out.writeShort(entry.getValue().size());
                for (String name : entry.getValue()) out.writeUTF(name);
            }
        }
        byte[] result = buffer.toByteArray();
        if (result.length > MAX_BYTES) throw new IOException("Snapshot exceeds plugin messaging size limit");
        return result;
    }

    public static NetworkSnapshot decode(byte[] bytes) throws IOException {
        if (bytes == null || bytes.length < 6 || bytes.length > MAX_BYTES || !header(bytes, SNAPSHOT)) {
            throw new IOException("Invalid snapshot header");
        }
        try (var in = new DataInputStream(new ByteArrayInputStream(bytes))) {
            in.readInt();
            in.readUnsignedByte();
            in.readUnsignedByte();
            long revision = in.readLong();
            int total = in.readInt();
            int segments = in.readInt();
            String filled = in.readUTF();
            String empty = in.readUTF();
            int timeoutSeconds = in.readInt();
            int serverCount = in.readUnsignedShort();
            if (serverCount > MAX_SERVERS) throw new IOException("Too many servers");
            Map<String, ServerSnapshot> servers = new LinkedHashMap<>();
            for (int i = 0; i < serverCount; i++) {
                String name = in.readUTF();
                ServerSnapshot server = new ServerSnapshot(in.readUTF(), in.readInt(), in.readInt(), in.readUTF());
                if (name.isBlank() || servers.putIfAbsent(name, server) != null) {
                    throw new IOException("Duplicate or empty server");
                }
            }
            int groupCount = in.readUnsignedShort();
            if (groupCount > MAX_GROUPS) throw new IOException("Too many groups");
            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (int i = 0; i < groupCount; i++) {
                String groupName = in.readUTF();
                int count = in.readUnsignedShort();
                if (count > MAX_SERVERS) throw new IOException("Too many group members");
                List<String> members = new ArrayList<>();
                for (int j = 0; j < count; j++) members.add(in.readUTF());
                if (groupName.isBlank() || groups.putIfAbsent(groupName, members) != null) {
                    throw new IOException("Duplicate or empty group");
                }
            }
            if (in.available() != 0) throw new IOException("Trailing data");
            try {
                return new NetworkSnapshot(revision, total, servers, groups, segments, filled, empty, timeoutSeconds);
            } catch (IllegalArgumentException ex) {
                throw new IOException("Invalid snapshot data", ex);
            }
        }
    }
}
