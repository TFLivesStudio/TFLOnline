package com.tflives.tflonline.common;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

/**
 * Proxy-owned configuration. Discovery adds keys without overwriting custom settings.
 * Config never stores backend addresses or secrets.
 */
public final class NetworkConfig {
    private final Path file;
    private final Properties props = new Properties();

    public NetworkConfig(Path dataDirectory) throws IOException {
        Files.createDirectories(dataDirectory);
        file = dataDirectory.resolve("tflonline.properties");
        if (Files.isRegularFile(file)) {
            try (InputStream input = Files.newInputStream(file)) { props.load(input); }
        }
        boolean changed = putDefault("sync.seconds", "2");
        changed |= putDefault("cache.timeout.seconds", "15");
        changed |= putDefault("bar.segments", "20");
        changed |= putDefault("bar.filled", "▌");
        changed |= putDefault("bar.empty", "░");
        if (changed || !Files.isRegularFile(file)) save();
    }

    public synchronized void discover(Collection<String> names) throws IOException {
        boolean changed = false;
        for (String name : names) {
            // Server IDs come from the proxy. Don't allow them to escape their namespace.
            if (!name.matches("[A-Za-z0-9_-]{1,64}")) continue;
            changed |= putDefault("server." + name + ".alias", name);
            changed |= putDefault("server." + name + ".capacity", "0");
            changed |= putDefault("server." + name + ".state", "AUTO");
        }
        if (changed) save();
    }

    public synchronized NetworkSnapshot snapshot(long revision, int total, Map<String, Integer> counts) {
        Map<String, ServerSnapshot> servers = new LinkedHashMap<>();
        counts.entrySet().stream().sorted(Map.Entry.comparingByKey())
                .forEach(entry -> {
                    String name = entry.getKey();
                    String prefix = "server." + name + ".";
                    servers.put(name, new ServerSnapshot(
                            props.getProperty(prefix + "alias", name),
                            entry.getValue(), integer(prefix + "capacity", 0, 0, Integer.MAX_VALUE),
                            props.getProperty(prefix + "state", "AUTO").toUpperCase(Locale.ROOT)));
                });
        Map<String, List<String>> groups = new LinkedHashMap<>();
        for (String key : props.stringPropertyNames().stream().sorted().toList()) {
            if (key.startsWith("group.")) {
                String group = key.substring("group.".length()).toLowerCase(Locale.ROOT);
                if (group.isBlank()) continue;
                List<String> names = Arrays.stream(props.getProperty(key, "").split(","))
                        .map(String::trim).filter(s -> !s.isEmpty()).distinct().toList();
                groups.put(group, names);
            }
        }
        return new NetworkSnapshot(revision, total, servers, groups,
                integer("bar.segments", 20, 1, 100),
                props.getProperty("bar.filled", "▌"),
                props.getProperty("bar.empty", "░"));
    }

    public synchronized int syncSeconds() {
        return integer("sync.seconds", 2, 1, 60);
    }

    private boolean putDefault(String key, String value) {
        if (props.containsKey(key)) return false;
        props.setProperty(key, value);
        return true;
    }

    private int integer(String key, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(props.getProperty(key, String.valueOf(fallback)));
            return (value >= min && value <= max) ? value : fallback;
        } catch (NumberFormatException ex) {
            return fallback;
        }
    }

    private void save() throws IOException {
        try (OutputStream output = Files.newOutputStream(file)) {
            props.store(output, "TFLOnline proxy configuration. All settings are managed here.");
        }
    }
}
