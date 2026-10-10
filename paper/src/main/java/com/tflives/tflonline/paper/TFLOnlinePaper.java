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
    private TFLOnlineExpansion expansion;

    @Override
    public void onEnable() {
        Bukkit.getMessenger().registerIncomingPluginChannel(this, WireProtocol.CHANNEL, this);
        Bukkit.getMessenger().registerOutgoingPluginChannel(this, WireProtocol.CHANNEL);
        Bukkit.getPluginManager().registerEvents(this, this);
        if (Bukkit.getPluginManager().isPluginEnabled("PlaceholderAPI")) {
            expansion = new TFLOnlineExpansion(this);
            if (expansion.register()) {
                getLogger().info("Registered TFLOnline PlaceholderAPI expansion.");
            } else {
                getLogger().warning("Unable to register TFLOnline PlaceholderAPI expansion.");
            }
        } else {
            getLogger().warning("PlaceholderAPI not detected. Install it to expose placeholders.");
        }
    }

    @Override
    public void onDisable() {
        if (expansion != null) expansion.unregister();
        Bukkit.getMessenger().unregisterIncomingPluginChannel(this);
        Bukkit.getMessenger().unregisterOutgoingPluginChannel(this);
        cache.set(null);
    }

    @EventHandler
    public void onJoin(PlayerJoinEvent event) {
        Player player = event.getPlayer();
        player.getScheduler().runDelayed(this, task -> {
            if (player.isOnline()) player.sendPluginMessage(this, WireProtocol.CHANNEL, request);
        }, null, 20L);
        player.getScheduler().runDelayed(this, task -> {
            CachedSnapshot current = cache.get();
            if (player.isOnline() && (current == null || !current.isFresh())) {
                player.sendPluginMessage(this, WireProtocol.CHANNEL, request);
            }
        }, null, 80L);
    }

    @Override
    public void onPluginMessageReceived(String channel, Player player, byte[] message) {
        if (!WireProtocol.CHANNEL.equals(channel)) return;
        try {
            NetworkSnapshot snapshot = WireProtocol.decode(message);
            long now = System.nanoTime();
            cache.updateAndGet(previous -> {
                if (previous != null && previous.isFresh()
                        && snapshot.revision() < previous.snapshot().revision()) return previous;
                return new CachedSnapshot(snapshot, now);
            });
        } catch (IOException | IllegalArgumentException ex) {
            getLogger().warning("Rejected invalid sync packet: " + ex.getMessage());
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
            return TimeUnit.NANOSECONDS.toSeconds(Math.max(0, System.nanoTime() - receivedAtNanos))
                    < snapshot.cacheTimeoutSeconds();
        }
    }
}
