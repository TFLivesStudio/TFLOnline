package com.tflives.tflonline.paper;

import com.tflives.tflonline.common.NetworkSnapshot;
import com.tflives.tflonline.common.WireProtocol;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.Listener;
import org.bukkit.event.player.PlayerJoinEvent;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.plugin.messaging.PluginMessageListener;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

public final class TFLOnlinePaper extends JavaPlugin implements Listener, PluginMessageListener {
    private final AtomicReference<CachedSnapshot> cache = new AtomicReference<>();
    private final byte[] request = WireProtocol.request();

    @Override
    public void onEnable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(this, WireProtocol.CHANNEL, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, WireProtocol.CHANNEL);
        Bukkit.getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            new TFLOnlineExpansion(this).register();
            getLogger().info("Registered %tflonline_...% expansion.");
        } else {
            getLogger().warning("PlaceholderAPI is not installed; synchronization works, but no placeholders are available.");
        }
    }

    @Override
    public void onDisable() {
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
        cache.set(null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        // Messaging uses the player's actual connection; no port or Redis needed.
        event.getPlayer().sendPluginMessage(this, WireProtocol.CHANNEL, request);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!WireProtocol.CHANNEL.equals(channel)) return;
        try {
            NetworkSnapshot snapshot = WireProtocol.decode(message);
            cache.set(new CachedSnapshot(snapshot, System.nanoTime()));
        } catch (IOException | IllegalArgumentException ex) {
            getLogger().warning("Rejected invalid proxy sync packet: " + ex.getMessage());
        }
    }

    public CachedSnapshot current() {
        return cache.get();
    }

    public record CachedSnapshot(NetworkSnapshot snapshot, long receivedAtNanos) {
        public long ageSeconds() {
            return TimeUnit.NANOSECONDS.toSeconds(Math.max(0, System.nanoTime() - receivedAtNanos));
        }

        public boolean isFresh() {
            return TimeUnit.NANOSECONDS.toSeconds(Math.max(0, System.nanoTime() - receivedAtNanos)) < snapshot.cacheTimeoutSeconds();
        }
    }
}
