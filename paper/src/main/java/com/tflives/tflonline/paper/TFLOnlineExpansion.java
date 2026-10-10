package com.tflives.tflonline.paper;

import com.tflives.tflonline.common.NetworkSnapshot;
import com.tflives.tflonline.common.ServerSnapshot;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/** PAPI parser is a pure, allocation-light read of the latest validated snapshot. */
public final class TFLOnlineExpansion extends PlaceholderExpansion {
    private final TFLOnlinePaper plugin;

    public TFLOnlineExpansion(TFLOnlinePaper plugin) {
        this.plugin = plugin;
    }

    @Override public @NotNull String getIdentifier() { return "tflonline"; }
    @Override public @NotNull String getAuthor() { return "TFLivesStudio"; }
    @Override public @NotNull String getVersion() { return plugin.getDescription().getVersion(); }
    @Override public boolean persist() { return true; }

    @Override
    public String onRequest(OfflinePlayer player, @NotNull String params) {
        var cache = plugin.current();
        String key = params.toLowerCase(Locale.ROOT);
        if ("sync_status".equals(key)) {
            return cache == null ? "WAITING" : cache.isFresh() ? "CONNECTED" : "STALE";
        }
        if ("sync_age".equals(key)) {
            return cache == null ? "-1" : String.valueOf(cache.ageSeconds());
        }
        if (cache == null || !cache.isFresh()) return "N/A";
        NetworkSnapshot snapshot = cache.snapshot();
        if ("total".equals(key)) return String.valueOf(snapshot.total());

        // Names may contain underscores. Colon delimiters avoid ambiguity.
        String[] segments = params.split(":", -1);
        if (segments.length == 3 && segments[0].equalsIgnoreCase("server")
                && segments[2].equalsIgnoreCase("online")) {
            ServerSnapshot server = snapshot.findServer(segments[1]);
            return server == null ? "N/A" : String.valueOf(server.players());
        }
        if (segments.length == 3 && segments[0].equalsIgnoreCase("group")
                && segments[2].equalsIgnoreCase("online")) {
            Integer count = snapshot.groupCount(segments[1]);
            return count == null ? "N/A" : String.valueOf(count);
        }
        if (segments.length == 2 && segments[0].equalsIgnoreCase("bar")) {
            String bar = snapshot.bar(segments[1]);
            return bar == null ? "N/A" : bar;
        }
        return null;
    }
}
