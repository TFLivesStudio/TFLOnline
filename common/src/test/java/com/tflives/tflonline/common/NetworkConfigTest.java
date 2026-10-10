package com.tflives.tflonline.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Properties;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConfigTest {
    @TempDir Path temp;

    @Test
    void generatesCommentedYamlAndDiscoversWithoutOverwritingSettings() throws Exception {
        NetworkConfig config = new NetworkConfig(temp);
        Path file = temp.resolve("config.yml");
        assertTrue(Files.readString(file).contains("# 06. SERVIDORES"));
        config.discover(List.of("lobby", "skyblock"));
        String text = Files.readString(file);
        assertTrue(text.contains("  lobby:"));
        Files.writeString(file, text.replace("alias: \"lobby\"", "alias: \"spawn\""));
        config.reload();
        config.discover(List.of("lobby", "skyblock", "gens"));
        NetworkSnapshot snap = config.snapshot(5, 13, Map.of("lobby", 4, "skyblock", 9, "gens", 0));
        assertNotNull(snap.findServer("spawn"));
        assertEquals(4, snap.findServer("lobby").players());
        assertNotNull(snap.findServer("gens"));
        assertTrue(Files.readString(file).contains("alias: \"spawn\""));
        assertTrue(Files.readString(file).contains("# 06. SERVIDORES"));
    }

    @Test
    void supportsServerGroupsAndCustomCapacities() throws Exception {
        NetworkConfig config = new NetworkConfig(temp);
        config.discover(List.of("skyblock", "survival"));
        Path file = temp.resolve("config.yml");
        String text = Files.readString(file).replace("groups: {}", "groups:\n  survival:\n    - skyblock\n    - survival");
        text = text.replace("skyblock:\n    alias: \"skyblock\"\n    capacity: 0",
                "skyblock:\n    alias: \"skyblock\"\n    capacity: 100");
        Files.writeString(file, text);
        config.reload();
        NetworkSnapshot snap = config.snapshot(1, 10, Map.of("skyblock", 6, "survival", 4));
        assertEquals(10, snap.groupCount("survival"));
        assertNotNull(snap.bar("skyblock"));
        assertEquals(15, snap.cacheTimeoutSeconds());
    }

    @Test
    void invalidReloadRetainsPreviousValidConfiguration() throws Exception {
        NetworkConfig config = new NetworkConfig(temp);
        int before = config.syncSeconds();
        Files.writeString(temp.resolve("config.yml"), "config-version: 1\nsynchronization: [invalid]\n");
        assertThrows(IOException.class, config::reload);
        assertEquals(before, config.syncSeconds());
    }

    @Test
    void duplicateYamlKeysAreRejected() throws Exception {
        NetworkConfig config = new NetworkConfig(temp);
        Files.writeString(temp.resolve("config.yml"), Files.readString(temp.resolve("config.yml")) + "\ngeneral:\n  language: en\n");
        assertThrows(IOException.class, config::reload);
    }

    @Test
    void upgradesLegacyPropertiesAndLeavesOriginalBackup() throws Exception {
        Properties old = new Properties();
        old.setProperty("sync.seconds", "4");
        old.setProperty("server.lobby.alias", "spawn");
        old.setProperty("server.lobby.capacity", "120");
        old.setProperty("group.hubs", "lobby");
        try (var out = Files.newOutputStream(temp.resolve("tflonline.properties"))) {
            old.store(out, "legacy");
        }
        NetworkConfig migrated = new NetworkConfig(temp);
        assertEquals(4, migrated.syncSeconds());
        assertEquals("spawn", migrated.snapshot(1, 8, Map.of("lobby", 8)).findServer("lobby").alias());
        assertEquals(8, migrated.snapshot(1, 8, Map.of("lobby", 8)).groupCount("hubs"));
        assertTrue(Files.exists(temp.resolve("tflonline.properties")));
    }
    @Test
    void doesNotCreateConflictingAliasesOnDiscovery() throws Exception {
        NetworkConfig config = new NetworkConfig(temp);
        config.discover(List.of("hub"));
        Path file = temp.resolve("config.yml");
        Files.writeString(file, Files.readString(file).replace("alias: \"hub\"", "alias: \"lobby\""));
        config.reload();
        config.discover(List.of("hub", "lobby"));
        NetworkSnapshot snapshot = config.snapshot(1, 3, Map.of("hub", 1, "lobby", 2));
        assertEquals(1, snapshot.findServer("hub").players());
        assertEquals(2, snapshot.findServer("lobby").players());
        assertEquals(2, snapshot.findServer("lobby-2").players());
    }

    @Test
    void rejectsUnsupportedAutomaticDeletion() throws Exception {
        NetworkConfig config = new NetworkConfig(temp);
        Path file = temp.resolve("config.yml");
        Files.writeString(file, Files.readString(file).replace("preserve-missing: true", "preserve-missing: false"));
        assertThrows(IOException.class, config::reload);
    }

}
