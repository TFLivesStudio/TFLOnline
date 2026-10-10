package com.tflives.tflonline.common;

import org.yaml.snakeyaml.LoaderOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.constructor.SafeConstructor;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;

public final class NetworkConfig {
    private static final String TEMPLATE = "/tflonline-config.yml";
    private final Path file;
    private volatile Settings settings;

    public NetworkConfig(Path directory) throws IOException {
        Files.createDirectories(directory);
        this.file = directory.resolve("config.yml");
        if (Files.notExists(file)) {
            String initial = readTemplate();
            Path old = directory.resolve("tflonline.properties");
            if (Files.isRegularFile(old)) initial = migrate(initial, old);
            writeAtomically(initial);
        }
        reload();
    }

    public synchronized void reload() throws IOException {
        Settings replacement = parse(Files.readString(file, StandardCharsets.UTF_8));
        settings = replacement;
    }

    public synchronized void discover(Collection<String> registered) throws IOException {
        if (!settings.autoRegister()) return;
        Set<String> newNames = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
        for (String name : registered) {
            if (name == null || !name.matches("[A-Za-z0-9_.-]{1,64}")) continue;
            if (settings.servers().keySet().stream().noneMatch(s -> s.equalsIgnoreCase(name))) {
                newNames.add(name);
            }
        }
        if (newNames.isEmpty()) return;
        String current = Files.readString(file, StandardCharsets.UTF_8);
        StringBuilder additions = new StringBuilder();
        for (String name : newNames) {
            additions.append("  ").append(name).append(":\n")
                    .append("    alias: \"").append(name).append("\"\n")
                    .append("    capacity: 0\n")
                    .append("    state: AUTO\n\n");
        }
        String updated = insertEntries(current, "servers", additions.toString());
        Settings validated = parse(updated);
        writeAtomically(updated);
        settings = validated;
    }

    public NetworkSnapshot snapshot(long revision, int total, Map<String, Integer> counts) {
        Settings current = settings;
        Map<String, ServerSnapshot> servers = new LinkedHashMap<>();
        for (var entry : counts.entrySet()) {
            ServerSetting configured = current.servers().getOrDefault(entry.getKey(),
                    new ServerSetting(entry.getKey(), 0, "AUTO"));
            servers.put(entry.getKey(), new ServerSnapshot(configured.alias(), entry.getValue(),
                    configured.capacity(), configured.state()));
        }
        return new NetworkSnapshot(revision, total, servers, current.groups(),
                current.barSegments(), current.filled(), current.empty(), current.timeoutSeconds());
    }

    public int syncSeconds() { return settings.intervalSeconds(); }
    public boolean broadcastOnEvents() { return settings.broadcastOnEvents(); }
    public int debounceMillis() { return settings.debounceMillis(); }
    public boolean debug() { return settings.debug(); }
    public String language() { return settings.language(); }
    public int configuredServers() { return settings.servers().size(); }
    public int configuredGroups() { return settings.groups().size(); }

    private static String readTemplate() throws IOException {
        try (InputStream in = NetworkConfig.class.getResourceAsStream(TEMPLATE)) {
            if (in == null) throw new IOException("Missing config template: " + TEMPLATE);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    private static String migrate(String template, Path propertiesFile) throws IOException {
        Properties previous = new Properties();
        try (InputStream in = Files.newInputStream(propertiesFile)) {
            previous.load(in);
        }
        String translated = template;
        translated = replaceValue(translated, "broadcast-interval-seconds", previous.getProperty("sync.seconds"));
        translated = replaceValue(translated, "cache-timeout-seconds", previous.getProperty("cache.timeout.seconds"));
        translated = replaceValue(translated, "segments", previous.getProperty("bar.segments"));
        translated = replaceQuoted(translated, "filled", previous.getProperty("bar.filled"));
        translated = replaceQuoted(translated, "empty", previous.getProperty("bar.empty"));
        Map<String, Map<String, String>> byServer = new LinkedHashMap<>();
        for (String key : previous.stringPropertyNames()) {
            if (!key.startsWith("server.")) continue;
            String rest = key.substring(7);
            int sep = rest.lastIndexOf('.');
            if (sep <= 0) continue;
            String name = rest.substring(0, sep);
            String field = rest.substring(sep + 1);
            if (!name.matches("[A-Za-z0-9_.-]{1,64}")) continue;
            byServer.computeIfAbsent(name, ignored -> new LinkedHashMap<>()).put(field, previous.getProperty(key));
        }
        if (!byServer.isEmpty()) {
            StringBuilder additions = new StringBuilder();
            for (var entry : byServer.entrySet()) {
                Map<String, String> values = entry.getValue();
                additions.append("  ").append(entry.getKey()).append(":\n")
                        .append("    alias: \"").append(yamlQuote(values.getOrDefault("alias", entry.getKey()))).append("\"\n")
                        .append("    capacity: ").append(validInt(values.get("capacity"), 0, 0, Integer.MAX_VALUE)).append("\n")
                        .append("    state: ").append(validState(values.getOrDefault("state", "AUTO"))).append("\n\n");
            }
            translated = insertEntries(translated, "servers", additions.toString());
        }
        StringBuilder groups = new StringBuilder();
        for (String key : previous.stringPropertyNames()) {
            if (!key.startsWith("group.")) continue;
            String name = key.substring(6);
            if (!name.matches("[A-Za-z0-9_-]{1,64}")) continue;
            groups.append("  ").append(name).append(":\n");
            for (String member : previous.getProperty(key, "").split(",")) {
                String trimmed = member.trim();
                if (trimmed.matches("[A-Za-z0-9_.-]{1,64}")) {
                    groups.append("    - \"").append(trimmed).append("\"\n");
                }
            }
        }
        if (!groups.isEmpty()) translated = insertEntries(translated, "groups", groups.toString());
        return translated;
    }

    private static String replaceValue(String source, String name, String value) {
        if (value == null || !value.matches("[0-9]{1,9}")) return source;
        return source.replaceFirst("(?m)^(\\s*" + name + ":\\s*)[^\\r\\n]+$", "$1" + value);
    }

    private static String replaceQuoted(String source, String name, String value) {
        if (value == null || value.isBlank()) return source;
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile("(?m)^(\\s*" + name + ":\\s*)[^\\r\\n]+$")
                .matcher(source);
        if (!matcher.find()) return source;
        return matcher.replaceFirst("$1" + java.util.regex.Matcher.quoteReplacement("\"" + yamlQuote(value) + "\""));
    }

    private static String yamlQuote(String text) {
        return text.replace("\\", "\\\\").replace("\"", "\\\"")
                .replace("\r", "").replace("\n", "");
    }

    private static String insertEntries(String text, String section, String additions) throws IOException {
        String[] lines = text.split("(?<=\\n)", -1);
        int start = -1;
        for (int i = 0; i < lines.length; i++) {
            if (lines[i].matches("^" + section + ":\\s*(?:\\{\\})?\\s*(?:\\r?\\n)?$")) {
                start = i;
                break;
            }
        }
        if (start == -1) throw new IOException("Cannot auto-register: missing or unsupported '" + section + "' section");
        StringBuilder result = new StringBuilder();
        for (int i = 0; i < lines.length; i++) {
            if (i == start && lines[i].trim().equals(section + ": {}")) {
                result.append(section).append(":\n");
                result.append(additions);
                additions = "";
                continue;
            }
            if (i > start && !additions.isEmpty() && !lines[i].isBlank()
                    && !lines[i].stripLeading().startsWith("#")
                    && !Character.isWhitespace(lines[i].charAt(0))) {
                result.append(additions);
                additions = "";
            }
            result.append(lines[i]);
        }
        if (!additions.isEmpty()) {
            if (!result.isEmpty() && result.charAt(result.length() - 1) != '\n') result.append('\n');
            result.append(additions);
        }
        return result.toString();
    }

    private static Settings parse(String source) throws IOException {
        LoaderOptions opts = new LoaderOptions();
        opts.setAllowDuplicateKeys(false);
        opts.setMaxAliasesForCollections(10);
        opts.setCodePointLimit(1_000_000);
        opts.setNestingDepthLimit(20);
        try {
            Object yaml = new Yaml(new SafeConstructor(opts)).load(source);
            Map<String, Object> root = mapping(yaml, "root");
            int version = integer(root.get("config-version"), 1, 1, 1, "config-version");
            Map<String, Object> general = mapping(root.get("general"), "general");
            String language = string(general.get("language"), "es", "general.language").toLowerCase(Locale.ROOT);
            if (!language.equals("es") && !language.equals("en")) throw new IllegalArgumentException("general.language must be 'es' or 'en'");
            boolean debug = bool(general.get("debug"), false, "general.debug");
            Map<String, Object> sync = mapping(root.get("synchronization"), "synchronization");
            if (!"plugin-messaging".equals(string(sync.get("transport"), "plugin-messaging", "synchronization.transport"))) {
                throw new IllegalArgumentException("Only plugin-messaging is currently supported");
            }
            int interval = integer(sync.get("broadcast-interval-seconds"), 3, 1, 60, "synchronization.broadcast-interval-seconds");
            int timeout = integer(sync.get("cache-timeout-seconds"), 15, 2, 300, "synchronization.cache-timeout-seconds");
            if (timeout <= interval) throw new IllegalArgumentException("cache-timeout-seconds must exceed broadcast-interval-seconds");
            boolean onEvents = bool(sync.get("broadcast-on-events"), true, "synchronization.broadcast-on-events");
            int debounce = integer(sync.get("event-debounce-millis"), 150, 0, 2000, "synchronization.event-debounce-millis");
            Map<String, Object> discovery = mapping(root.get("discovery"), "discovery");
            boolean auto = bool(discovery.get("auto-register"), true, "discovery.auto-register");
            bool(discovery.get("preserve-missing"), true, "discovery.preserve-missing");
            Map<String, Object> bar = mapping(mapping(root.get("bars"), "bars").get("default"), "bars.default");
            int segments = integer(bar.get("segments"), 20, 1, 100, "bars.default.segments");
            String filled = string(bar.get("filled"), "▌", "bars.default.filled");
            String empty = string(bar.get("empty"), "░", "bars.default.empty");
            if (filled.isBlank() || empty.isBlank() || filled.length() > 12 || empty.length() > 12) {
                throw new IllegalArgumentException("Bar characters must be 1-12 code units");
            }
            Map<String, List<String>> groups = new LinkedHashMap<>();
            for (var entry : mapping(root.get("groups"), "groups").entrySet()) {
                String name = entry.getKey().toLowerCase(Locale.ROOT);
                if (!name.matches("[a-z0-9_-]{1,64}")) throw new IllegalArgumentException("Invalid group name: " + name);
                if (!(entry.getValue() instanceof List<?> sequence)) throw new IllegalArgumentException("groups." + name + " must be a list");
                List<String> members = new ArrayList<>();
                for (Object member : sequence) {
                    String id = string(member, "", "groups." + name);
                    if (!id.matches("[A-Za-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Invalid member: " + id);
                    members.add(id);
                }
                if (groups.putIfAbsent(name, List.copyOf(members)) != null) throw new IllegalArgumentException("Duplicate group: " + name);
            }
            Map<String, ServerSetting> servers = new LinkedHashMap<>();
            Set<String> aliases = new TreeSet<>(String.CASE_INSENSITIVE_ORDER);
            for (var entry : mapping(root.get("servers"), "servers").entrySet()) {
                String id = entry.getKey();
                if (!id.matches("[A-Za-z0-9_.-]{1,64}")) throw new IllegalArgumentException("Invalid backend ID: " + id);
                Map<String, Object> conf = mapping(entry.getValue(), "servers." + id);
                String alias = string(conf.get("alias"), id, "servers." + id + ".alias");
                if (!alias.matches("[A-Za-z0-9_.-]{1,64}") || !aliases.add(alias)) throw new IllegalArgumentException("Duplicated or invalid alias: " + alias);
                int capacity = integer(conf.get("capacity"), 0, 0, Integer.MAX_VALUE, "servers." + id + ".capacity");
                String state = validState(string(conf.get("state"), "AUTO", "servers." + id + ".state"));
                if (servers.putIfAbsent(id, new ServerSetting(alias, capacity, state)) != null) {
                    throw new IllegalArgumentException("Duplicated backend: " + id);
                }
            }
            if (servers.size() > 500 || groups.size() > 200) throw new IllegalArgumentException("Configuration exceeds protocol limits");
            return new Settings(language, debug, interval, timeout, onEvents, debounce,
                    auto, segments, filled, empty, Map.copyOf(servers), Map.copyOf(groups));
        } catch (RuntimeException ex) {
            throw new IOException("Invalid TFLOnline config.yml: " + ex.getMessage(), ex);
        }
    }

    private static Map<String, Object> mapping(Object obj, String path) {
        if (obj == null) return Map.of();
        if (!(obj instanceof Map<?, ?> source)) throw new IllegalArgumentException(path + " must be a YAML map");
        Map<String, Object> result = new LinkedHashMap<>();
        for (var entry : source.entrySet()) {
            if (!(entry.getKey() instanceof String key)) throw new IllegalArgumentException(path + " has a non-string key");
            result.put(key, entry.getValue());
        }
        return result;
    }

    private static String string(Object value, String fallback, String path) {
        if (value == null) return fallback;
        if (!(value instanceof String text)) throw new IllegalArgumentException(path + " must be a string");
        return text;
    }

    private static boolean bool(Object value, boolean fallback, String path) {
        if (value == null) return fallback;
        if (!(value instanceof Boolean flag)) throw new IllegalArgumentException(path + " must be true/false");
        return flag;
    }

    private static int integer(Object value, int fallback, int min, int max, String path) {
        if (value == null) return fallback;
        if (!(value instanceof Integer || value instanceof Long)) throw new IllegalArgumentException(path + " must be an integer");
        long number = ((Number) value).longValue();
        if (number < min || number > max) throw new IllegalArgumentException(path + " must be between " + min + " and " + max);
        return (int) number;
    }

    private static int validInt(String str, int fallback, int min, int max) {
        try {
            int value = Integer.parseInt(str);
            return value >= min && value <= max ? value : fallback;
        } catch (NumberFormatException | NullPointerException ex) {
            return fallback;
        }
    }

    private static String validState(String state) {
        String normal = Objects.requireNonNull(state).toUpperCase(Locale.ROOT);
        if (!Set.of("AUTO", "ONLINE", "OFFLINE", "MAINTENANCE", "STARTING", "UNKNOWN").contains(normal)) {
            throw new IllegalArgumentException("Invalid server state: " + state);
        }
        return normal;
    }

    private void writeAtomically(String content) throws IOException {
        Path temp = Files.createTempFile(file.getParent(), "tflonline-", ".tmp");
        try {
            Files.writeString(temp, content, StandardCharsets.UTF_8);
            try {
                Files.move(temp, file, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
            } catch (AtomicMoveNotSupportedException ex) {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
            }
        } finally {
            Files.deleteIfExists(temp);
        }
    }

    private record ServerSetting(String alias, int capacity, String state) {}
    private record Settings(String language, boolean debug, int intervalSeconds, int timeoutSeconds,
                            boolean broadcastOnEvents, int debounceMillis, boolean autoRegister,
                            int barSegments, String filled, String empty,
                            Map<String, ServerSetting> servers, Map<String, List<String>> groups) {}
}
