package com.tflives.tflonline.common;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class WireProtocolTest {
    private final NetworkSnapshot sample = new NetworkSnapshot(4, 17, Map.of(
            "lobby1", new ServerSnapshot("lobby", 7, 25, "AUTO"),
            "skyblock", new ServerSnapshot("skyblock", 10, 20, "MAINTENANCE")
    ), Map.of("lobbies", List.of("lobby1"), "survival", List.of("skyblock")), 10, "▌", "░");

    @Test void roundTrip() throws Exception {
        NetworkSnapshot read = WireProtocol.decode(WireProtocol.encode(sample));
        assertEquals(sample, read);
        assertEquals(17, read.total());
        assertEquals(7, read.findServer("lobby").players());
        assertEquals(10, read.groupCount("survival"));
        assertEquals("▌▌▌▌▌░░░░░", read.bar("skyblock"));
    }

    @Test void requestsAreNotSnapshots() {
        assertTrue(WireProtocol.isRequest(WireProtocol.request()));
        assertThrows(IOException.class, () -> WireProtocol.decode(WireProtocol.request()));
    }

    @Test void rejectsMalformedOrExtendedFrames() throws Exception {
        byte[] encoded = WireProtocol.encode(sample);
        encoded[0] = 0;
        assertThrows(IOException.class, () -> WireProtocol.decode(encoded));
        byte[] trailing = java.util.Arrays.copyOf(WireProtocol.encode(sample), WireProtocol.encode(sample).length + 1);
        assertThrows(IOException.class, () -> WireProtocol.decode(trailing));
    }

    @Test void groupAliasesDeduplicate() {
        NetworkSnapshot s = new NetworkSnapshot(1, 2,
                Map.of("hub1", new ServerSnapshot("lobby", 2, 0, "AUTO")),
                Map.of("lobbies", List.of("hub1", "lobby")), 4, "█", "░");
        assertEquals(2, s.groupCount("lobbies"));
        assertNull(s.bar("hub1"));
    }
}
