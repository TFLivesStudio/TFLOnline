package com.tflives.tflonline.bungee;

import com.tflives.tflonline.common.NetworkConfig;
import com.tflives.tflonline.common.WireProtocol;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

public final class TFLOnlineBungee extends Plugin implements Listener {
    private NetworkConfig config;
    private final AtomicLong revision = new AtomicLong();

    @Override
    public void onEnable() {
        try {
            config = new NetworkConfig(getDataFolder().toPath());
            discover();
        } catch (IOException ex) {
            getLogger().severe("Unable to load TFLOnline configuration: " + ex.getMessage());
            return;
        }
        getProxy().registerChannel(WireProtocol.CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getScheduler().schedule(this, this::broadcast, 1, config.syncSeconds(), TimeUnit.SECONDS);
        getLogger().info("TFLOnline started with plugin messaging (no extra port).");
    }

    @Override
    public void onDisable() {
        getProxy().unregisterChannel(WireProtocol.CHANNEL);
    }

    @EventHandler
    public void onMessage(PluginMessageEvent event) {
        if (!WireProtocol.CHANNEL.equals(event.getTag())) return;
        event.setCancelled(true);
        if (config == null || !(event.getSender() instanceof Server server)) return;
        if (WireProtocol.isRequest(event.getData())) sendTo(server.getInfo());
    }

    @EventHandler
    public void onServerSwitch(ServerConnectedEvent event) {
        if (config != null) broadcast();
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        if (config != null) {
            getProxy().getScheduler().schedule(this, this::broadcast, 150, TimeUnit.MILLISECONDS);
        }
    }

    private void discover() {
        try {
            config.discover(getProxy().getServers().keySet());
        } catch (IOException ex) {
            getLogger().warning("Unable to persist discovered servers: " + ex.getMessage());
        }
    }

    private byte[] snapshot() {
        discover();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ServerInfo server : getProxy().getServers().values()) {
            counts.put(server.getName(), server.getPlayers().size());
        }
        try {
            return WireProtocol.encode(config.snapshot(revision.incrementAndGet(),
                    getProxy().getPlayers().size(), counts));
        } catch (IOException ex) {
            getLogger().warning("Unable to encode TFLOnline snapshot: " + ex.getMessage());
            return null;
        }
    }

    private void sendTo(ServerInfo server) {
        byte[] data = snapshot();
        if (data != null) server.sendData(WireProtocol.CHANNEL, data, false);
    }

    private void broadcast() {
        byte[] data = snapshot();
        if (data == null) return;
        for (ServerInfo server : getProxy().getServers().values()) {
            if (!server.getPlayers().isEmpty()) server.sendData(WireProtocol.CHANNEL, data, false);
        }
    }
}
