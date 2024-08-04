package com.jannik_kuehn.loritimebukkit.placeholder;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

@SuppressFBWarnings("HE_INHERITS_EQUALS_USE_HASHCODE")
public class LoriTimePlaceholder extends PlaceholderExpansion {

    private final LoriTimePlugin plugin;

    private final TimeStorage timeStorage;

    public LoriTimePlaceholder(final LoriTimePlugin plugin, final TimeStorage timeStorage) {
        super();
        this.plugin = plugin;
        this.timeStorage = timeStorage;
    }

    @Override
    public String onRequest(final OfflinePlayer player, @NotNull final String params) {
        return switch (params) {
            case "unformatted_onlinetime" -> String.valueOf(getUnformattedOnlineTime(player));
            case "formatted_onlinetime" -> getFormattedOnlineTime(player);
            case "afk" -> {
                if (plugin.isAfkEnabled()) {
                    yield String.valueOf(plugin.getAfkStatusProvider().getRealPlayer(new LoriTimePlayer(player.getUniqueId(), player.getName())).isAfk());
                }
                yield "Feature not enabled!";
            }
            default -> "Nothing Found";
        };
    }

    private long getUnformattedOnlineTime(final OfflinePlayer player) {
        long onlineTime = 0;
        try {
            final OptionalLong optionalLong = timeStorage.getTime(player.getUniqueId());
            if (optionalLong.isPresent()) {
                onlineTime = optionalLong.getAsLong();
            }
        } catch (final StorageException e) {
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
        return plugin.getServer().getPluginVersion();
    }
}
