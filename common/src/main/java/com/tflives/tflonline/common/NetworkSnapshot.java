package com.tflives.tflonline.common;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/** An immutable, single-version view of the network. */
public record NetworkSnapshot(long revision, int total, Map<String, ServerSnapshot> servers,
                              Map<String, List<String>> groups, int barSegments,
                              String barFilled, String barEmpty, int cacheTimeoutSeconds) {
    public NetworkSnapshot {
        if (revision < 0 || total < 0 || barSegments < 1 || barSegments > 100 || cacheTimeoutSeconds < 2 || cacheTimeoutSeconds > 300) {
            throw new IllegalArgumentException("Invalid snapshot header");
        }
        Objects.requireNonNull(servers, "servers");
        Objects.requireNonNull(groups, "groups");
        servers = Map.copyOf(servers);
        Map<String, List<String>> copy = new LinkedHashMap<>();
        groups.forEach((key, value) -> copy.put(key, List.copyOf(value)));
        groups = Map.copyOf(copy);
        Objects.requireNonNull(barFilled, "barFilled");
        Objects.requireNonNull(barEmpty, "barEmpty");
    }

    public ServerSnapshot findServer(String nameOrAlias) {
        ServerSnapshot direct = servers.get(nameOrAlias);
        if (direct != null) return direct;
        for (ServerSnapshot server : servers.values()) {
            if (server.alias().equalsIgnoreCase(nameOrAlias)) return server;
        }
        for (var entry : servers.entrySet()) {
            if (entry.getKey().equalsIgnoreCase(nameOrAlias)) return entry.getValue();
        }
        return null;
    }

    public Integer groupCount(String group) {
        List<String> members = groups.get(group.toLowerCase(Locale.ROOT));
        if (members == null) return null;
        int sum = 0;
        java.util.Set<ServerSnapshot> unique = java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<>());
        for (String name : members) {
            ServerSnapshot server = findServer(name);
            if (server != null && unique.add(server)) sum += server.players();
        }
        return sum;
    }

    public String bar(String serverName) {
        ServerSnapshot server = findServer(serverName);
        if (server == null || server.capacity() <= 0) return null;
        int segments = Math.max(0, Math.min(barSegments,
                (int) Math.floor((double) server.players() * barSegments / server.capacity())));
        return barFilled.repeat(segments) + barEmpty.repeat(barSegments - segments);
    }
}
