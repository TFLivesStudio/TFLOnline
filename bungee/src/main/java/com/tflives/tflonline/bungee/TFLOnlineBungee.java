package com.tflives.tflonline.bungee;

import com.tflives.tflonline.common.Messages;
import com.tflives.tflonline.common.NetworkConfig;
import com.tflives.tflonline.common.WireProtocol;
import net.md_5.bungee.api.config.ServerInfo;
import net.md_5.bungee.api.connection.Server;
import net.md_5.bungee.api.event.PlayerDisconnectEvent;
import net.md_5.bungee.api.event.PluginMessageEvent;
import net.md_5.bungee.api.event.ServerConnectedEvent;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.Listener;
import net.md_5.bungee.api.plugin.Plugin;
import net.md_5.bungee.event.EventHandler;
import net.md_5.bungee.api.chat.TextComponent;

import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

public final class TFLOnlineBungee extends Plugin implements Listener {
    private volatile NetworkConfig config;
    private final AtomicLong revision = new AtomicLong();
    private final AtomicBoolean eventQueued = new AtomicBoolean();
    private volatile long nextBroadcastNanos;

    @Override
    public void onEnable() {
        try {
            config = new NetworkConfig(getDataFolder().toPath());
            discover();
        } catch (IOException ex) {
            getLogger().severe("Unable to initialize TFLOnline: " + ex.getMessage());
            return;
        }
        getProxy().registerChannel(WireProtocol.CHANNEL);
        getProxy().getPluginManager().registerListener(this, this);
        getProxy().getPluginManager().registerCommand(this, new AdminCommand());
        nextBroadcastNanos = System.nanoTime();
        getProxy().getScheduler().schedule(this, this::periodic, 1, 1, TimeUnit.SECONDS);
        getLogger().info("TFLOnline ready: Plugin Messaging, no additional listener port.");
    }

    @Override
    public void onDisable() {
        if (config != null) getProxy().unregisterChannel(WireProtocol.CHANNEL);
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
        queueEvent();
    }

    @EventHandler
    public void onDisconnect(PlayerDisconnectEvent event) {
        queueEvent();
    }

    private void queueEvent() {
        NetworkConfig current = config;
        if (current == null || !current.broadcastOnEvents() || !eventQueued.compareAndSet(false, true)) return;
        getProxy().getScheduler().schedule(this, () -> {
            eventQueued.set(false);
            broadcast();
        }, Math.max(1, current.debounceMillis()), TimeUnit.MILLISECONDS);
    }

    private void periodic() {
        NetworkConfig current = config;
        if (current == null) return;
        long now = System.nanoTime();
        if (now < nextBroadcastNanos) return;
        nextBroadcastNanos = now + TimeUnit.SECONDS.toNanos(current.syncSeconds());
        broadcast();
    }

    private void discover() {
        NetworkConfig current = config;
        if (current == null) return;
        try {
            current.discover(getProxy().getServers().keySet());
        } catch (IOException ex) {
            getLogger().warning("Could not save discovered backends: " + ex.getMessage());
        }
    }

    private byte[] snapshot() {
        NetworkConfig current = config;
        if (current == null) return null;
        discover();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (ServerInfo server : getProxy().getServers().values()) {
            counts.put(server.getName(), server.getPlayers().size());
        }
        try {
            return WireProtocol.encode(current.snapshot(revision.incrementAndGet(), getProxy().getPlayers().size(), counts));
        } catch (IOException | IllegalArgumentException ex) {
            getLogger().warning("Could not encode snapshot: " + ex.getMessage());
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

    private final class AdminCommand extends Command {
        private AdminCommand() {
            super("tflonline", "tflonline.admin");
        }

        @Override
        public void execute(net.md_5.bungee.api.CommandSender sender, String[] args) {
            NetworkConfig current = config;
            if (current == null) {
                sender.sendMessage(new TextComponent(Messages.text("es", "disabled")));
                return;
            }
            String sub = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
            switch (sub) {
                case "reload" -> {
                    try {
                        current.reload();
                        discover();
                        nextBroadcastNanos = 0;
                        broadcast();
                        sender.sendMessage(new TextComponent(Messages.text(current.language(), "reloaded")));
                    } catch (IOException ex) {
                        sender.sendMessage(new TextComponent(Messages.text(current.language(), "reload-failed") + ex.getMessage()));
                    }
                }
                case "sync" -> {
                    broadcast();
                    sender.sendMessage(new TextComponent(Messages.text(current.language(), "sync")));
                }
                case "status" -> sender.sendMessage(new TextComponent("TFLOnline | "
                        + getProxy().getPlayers().size() + " online | "
                        + getProxy().getServers().size() + " registered | "
                        + current.configuredGroups() + " groups | " + current.syncSeconds() + "s interval"));
                case "servers" -> {
                    if (getProxy().getServers().isEmpty()) sender.sendMessage(new TextComponent(Messages.text(current.language(), "none")));
                    for (ServerInfo server : getProxy().getServers().values()) {
                        sender.sendMessage(new TextComponent(server.getName() + ": " + server.getPlayers().size()));
                    }
                }
                case "groups" -> sender.sendMessage(new TextComponent("TFLOnline: "
                        + current.configuredGroups() + " configured backend groups"));
                case "debug" -> sender.sendMessage(new TextComponent("TFLOnline debug=" + current.debug()));
                default -> sender.sendMessage(new TextComponent(Messages.text(current.language(), "help")));
            }
        }
    }
}
