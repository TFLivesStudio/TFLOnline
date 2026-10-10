package com.tflives.tflonline.common;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class NetworkConfigTest {
    @TempDir Path temp;

    @Test void discoveringNewServersDoesNotOverwriteExistingSettings() throws Exception {
        NetworkConfig config = new NetworkConfig(temp);
        config.discover(java.util.List.of("lobby", "skyblock"));
        Path file = temp.resolve("tflonline.properties");
        String data = Files.readString(file);
        assertTrue(data.contains("server.lobby.alias=lobby"));
        Files.writeString(file, data.replace("server.lobby.alias=lobby", "server.lobby.alias=spawn"));
        config = new NetworkConfig(temp);
        config.discover(java.util.List.of("lobby", "skyblock", "gens"));
        NetworkSnapshot s = config.snapshot(1, 0, Map.of("lobby", 0, "skyblock", 0, "gens", 0));
        assertNotNull(s.findServer("spawn"));
        assertNotNull(s.findServer("gens"));
        assertEquals("spawn", s.findServer("lobby").alias());
    }
}
