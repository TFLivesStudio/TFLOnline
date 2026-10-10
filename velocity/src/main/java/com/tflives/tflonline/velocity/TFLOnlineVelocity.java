package com.tflives.tflonline.velocity;

import com.google.inject.Inject;
import com.tflives.tflonline.common.Messages;
import com.tflives.tflonline.common.NetworkConfig;
import com.tflives.tflonline.common.RequestThrottle;
import com.tflives.tflonline.common.WireProtocol;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.event.Subscribe;
import com.velocitypowered.api.event.connection.DisconnectEvent;
import com.velocitypowered.api.event.connection.PluginMessageEvent;
import com.velocitypowered.api.event.player.ServerPostConnectEvent;
import com.velocitypowered.api.event.proxy.ProxyInitializeEvent;
import com.velocitypowered.api.plugin.Plugin;
import com.velocitypowered.api.plugin.annotation.DataDirectory;
import com.velocitypowered.api.proxy.ProxyServer;
import com.velocitypowered.api.proxy.ServerConnection;
import com.velocitypowered.api.proxy.messages.MinecraftChannelIdentifier;
import com.velocitypowered.api.proxy.server.RegisteredServer;
import net.kyori.adventure.text.Component;
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

@Plugin(id = "tflonline", name = "TFLOnline", version = "0.2.0-SNAPSHOT",
        description = "Cross-server online counters for PlaceholderAPI", authors = {"tflives"})
public final class TFLOnlineVelocity {
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(WireProtocol.CHANNEL);
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path directory;
    private final AtomicLong revision = new AtomicLong();
    private final RequestThrottle requestThrottle = new RequestThrottle(500);
    private final AtomicBoolean eventQueued = new AtomicBoolean();
    private volatile long nextBroadcastNanos;
    private volatile NetworkConfig config;

    @Inject
    public TFLOnlineVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path directory) {
        this.proxy = proxy;
        this.logger = logger;
        this.directory = directory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        try {
            config = new NetworkConfig(directory);
            discover();
        } catch (IOException ex) {
            logger.error("Unable to initialize TFLOnline configuration", ex);
            return;
        }
        proxy.getChannelRegistrar().register(CHANNEL);
        proxy.getCommandManager().register(
                proxy.getCommandManager().metaBuilder("tflonline").plugin(this).build(),
                new AdminCommand());
        nextBroadcastNanos = System.nanoTime();
        proxy.getScheduler().buildTask(this, this::periodic)
                .delay(Duration.ofSeconds(1))
                .repeat(Duration.ofSeconds(1))
                .schedule();
        logger.info("TFLOnline ready: Plugin Messaging, no additional listener port.");
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        queueEvent();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        queueEvent();
    }

    @Subscribe
    public void onMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) return;
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (config == null || !(event.getSource() instanceof ServerConnection connection)) return;
        if (WireProtocol.isRequest(event.getData())
                && requestThrottle.allow(connection.getServer().getServerInfo().getName())) {
            sendTo(connection.getServer());
        }
    }

    private void queueEvent() {
        NetworkConfig current = config;
        if (current == null || !current.broadcastOnEvents() || !eventQueued.compareAndSet(false, true)) return;
        proxy.getScheduler().buildTask(this, () -> {
            eventQueued.set(false);
            broadcast();
        }).delay(Duration.ofMillis(Math.max(1, current.debounceMillis()))).schedule();
    }

    private void periodic() {
        NetworkConfig current = config;
        if (current == null) return;
        long now = System.nanoTime();
        if (now < nextBroadcastNanos) return;
        nextBroadcastNanos = now + Duration.ofSeconds(current.syncSeconds()).toNanos();
        broadcast();
    }

    private void discover() {
        NetworkConfig current = config;
        if (current == null) return;
        try {
            current.discover(proxy.getAllServers().stream().map(s -> s.getServerInfo().getName()).toList());
        } catch (IOException ex) {
            logger.warn("Could not save newly discovered backends", ex);
        }
    }

    private byte[] snapshot() {
        NetworkConfig current = config;
        if (current == null) return null;
        discover();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RegisteredServer backend : proxy.getAllServers()) {
            counts.put(backend.getServerInfo().getName(), backend.getPlayersConnected().size());
        }
        try {
            return WireProtocol.encode(current.snapshot(revision.incrementAndGet(), proxy.getPlayerCount(), counts));
        } catch (IOException | IllegalArgumentException ex) {
            logger.warn("Could not encode TFLOnline snapshot", ex);
            return null;
        }
    }

    private void sendTo(RegisteredServer server) {
        byte[] data = snapshot();
        if (data != null) server.sendPluginMessage(CHANNEL, data);
    }

    private void broadcast() {
        byte[] data = snapshot();
        if (data == null) return;
        for (RegisteredServer backend : proxy.getAllServers()) {
            if (!backend.getPlayersConnected().isEmpty()) backend.sendPluginMessage(CHANNEL, data);
        }
    }

    private final class AdminCommand implements SimpleCommand {
        @Override
        public void execute(Invocation invocation) {
            var sender = invocation.source();
            NetworkConfig current = config;
            if (current == null) {
                sender.sendMessage(Component.text(Messages.text("es", "disabled")));
                return;
            }
            String[] args = invocation.arguments();
            String sub = args.length == 0 ? "" : args[0].toLowerCase(java.util.Locale.ROOT);
            switch (sub) {
                case "reload" -> {
                    try {
                        current.reload();
                        discover();
                        nextBroadcastNanos = 0;
                        broadcast();
                        sender.sendMessage(Component.text(Messages.text(current.language(), "reloaded")));
                    } catch (IOException ex) {
                        sender.sendMessage(Component.text(Messages.text(current.language(), "reload-failed") + ex.getMessage()));
                    }
                }
                case "sync" -> {
                    broadcast();
                    sender.sendMessage(Component.text(Messages.text(current.language(), "sync")));
                }
                case "status" -> sender.sendMessage(Component.text(
                        "TFLOnline | " + proxy.getPlayerCount() + " online | "
                                + proxy.getAllServers().size() + " registered | "
                                + current.configuredGroups() + " groups | " + current.syncSeconds() + "s interval"));
                case "servers" -> {
                    if (proxy.getAllServers().isEmpty()) sender.sendMessage(Component.text(Messages.text(current.language(), "none")));
                    for (RegisteredServer server : proxy.getAllServers()) {
                        sender.sendMessage(Component.text(server.getServerInfo().getName()
                                + ": " + server.getPlayersConnected().size()));
                    }
                }
                case "groups" -> sender.sendMessage(Component.text(
                        "TFLOnline: " + current.configuredGroups() + " configured backend groups"));
                case "debug" -> sender.sendMessage(Component.text("TFLOnline debug=" + current.debug()));
                default -> sender.sendMessage(Component.text(Messages.text(current.language(), "help")));
            }
        }

        @Override
        public boolean hasPermission(Invocation invocation) {
            return invocation.source().hasPermission("tflonline.admin");
        }
    }
}
