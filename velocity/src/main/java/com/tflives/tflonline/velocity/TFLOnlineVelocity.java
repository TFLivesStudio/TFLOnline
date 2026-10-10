package com.tflives.tflonline.velocity;

import com.google.inject.Inject;
import com.tflives.tflonline.common.NetworkConfig;
import com.tflives.tflonline.common.WireProtocol;
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
import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Plugin(id = "tflonline", name = "TFLOnline", version = "0.1.0-SNAPSHOT",
        description = "Player counts for PlaceholderAPI without another port", authors = {"TFLivesStudio"})
public final class TFLOnlineVelocity {
    private static final MinecraftChannelIdentifier CHANNEL = MinecraftChannelIdentifier.from(WireProtocol.CHANNEL);
    private final ProxyServer proxy;
    private final Logger logger;
    private final Path dataDirectory;
    private final AtomicLong revision = new AtomicLong();
    private NetworkConfig config;

    @Inject
    public TFLOnlineVelocity(ProxyServer proxy, Logger logger, @DataDirectory Path dataDirectory) {
        this.proxy = proxy;
        this.logger = logger;
        this.dataDirectory = dataDirectory;
    }

    @Subscribe
    public void onInitialize(ProxyInitializeEvent event) {
        try {
            config = new NetworkConfig(dataDirectory);
            discover();
        } catch (IOException ex) {
            logger.error("Unable to load proxy configuration", ex);
            return;
        }
        proxy.getChannelRegistrar().register(CHANNEL);
        proxy.getScheduler().buildTask(this, this::broadcast)
                .delay(Duration.ofSeconds(1))
                .repeat(Duration.ofSeconds(config.syncSeconds()))
                .schedule();
        logger.info("TFLOnline started with plugin messaging (no extra port).");
    }

    @Subscribe
    public void onServerSwitch(ServerPostConnectEvent event) {
        if (config != null) broadcast();
    }

    @Subscribe
    public void onDisconnect(DisconnectEvent event) {
        if (config != null) {
            proxy.getScheduler().buildTask(this, this::broadcast).delay(Duration.ofMillis(150)).schedule();
        }
    }

    @Subscribe
    public void onMessage(PluginMessageEvent event) {
        if (!CHANNEL.equals(event.getIdentifier())) return;
        // Always consume our channel: untrusted player payloads must never be forwarded.
        event.setResult(PluginMessageEvent.ForwardResult.handled());
        if (config == null || !(event.getSource() instanceof ServerConnection connection)) return;
        if (WireProtocol.isRequest(event.getData())) {
            sendTo(connection.getServer());
        }
    }

    private void discover() {
        try {
            config.discover(proxy.getAllServers().stream().map(s -> s.getServerInfo().getName()).toList());
        } catch (IOException ex) {
            logger.warn("Could not update discovered servers", ex);
        }
    }

    private byte[] snapshot() {
        discover();
        Map<String, Integer> counts = new LinkedHashMap<>();
        for (RegisteredServer server : proxy.getAllServers()) {
            counts.put(server.getServerInfo().getName(), server.getPlayersConnected().size());
        }
        try {
            return WireProtocol.encode(config.snapshot(revision.incrementAndGet(), proxy.getPlayerCount(), counts));
        } catch (IOException ex) {
            logger.warn("Could not encode snapshot; check server names and payload size", ex);
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
        for (RegisteredServer server : proxy.getAllServers()) {
            if (!server.getPlayersConnected().isEmpty()) server.sendPluginMessage(CHANNEL, data);
        }
    }
}
