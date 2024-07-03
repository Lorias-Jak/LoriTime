package com.jannik_kuehn.loritimebukkit.util;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

public class LoriTimePlaceholder extends PlaceholderExpansion {

    private final LoriTimePlugin plugin;

    public LoriTimePlaceholder(LoriTimePlugin plugin) {
        super();
        this.plugin = plugin;
    }

    @Override
    public String onRequest(final OfflinePlayer player, @NotNull final String params) {
        switch (params) {
            case "unformatted_onlinetime":
                return String.valueOf(getUnformattedOnlineTime(player));
            case "formatted_onlinetime":
                return getFormattedOnlineTime(player);
            case "afk":
                if (plugin.isAfkEnabled()) {
                    return String.valueOf(plugin.getAfkStatusProvider().getRealPlayer(new LoriTimePlayer(player.getUniqueId(), player.getName())).isAfk());
                }
            return "Feature not enabled!";
            default:
                return "Nothing Found";
        }
    }

    private long getUnformattedOnlineTime(final OfflinePlayer player) {
        long onlineTime = 0;
        try {
            OptionalLong optionalLong = plugin.getTimeStorage().getTime(player.getUniqueId());
            if (optionalLong.isPresent()) {
                onlineTime = optionalLong.getAsLong();
            }
        } catch (StorageException e) {
            plugin.getLogger().error("Error while getting the online time placeholder of player " + player.getName(), e);
        }
        return onlineTime;
    }

    private String getFormattedOnlineTime(final OfflinePlayer player) {
        return TimeUtil.formatTime(getUnformattedOnlineTime(player), plugin.getLocalization());
    }

    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    @Override
    public @NotNull String getIdentifier() {
        return "LoriTime";
    }

    @Override
    public @NotNull String getAuthor() {
        return "Lorias-Jak";
    }

    @Override
    public @NotNull String getVersion() {
        return plugin.getPluginVersion();
    }
}
