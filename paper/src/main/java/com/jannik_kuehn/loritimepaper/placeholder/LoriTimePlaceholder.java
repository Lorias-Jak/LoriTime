package com.jannik_kuehn.loritimepaper.placeholder;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
@SuppressFBWarnings("HE_INHERITS_EQUALS_USE_HASHCODE")
public class LoriTimePlaceholder extends PlaceholderExpansion {

    private final LoriTimePlugin loriTimePlugin;

    private final TimeStorage timeStorage;

    private final LoriTimeLogger log;

    private final Map<UUID, Long> offlinePlayerTime;

    public LoriTimePlaceholder(final LoriTimePlugin plugin, final TimeStorage timeStorage) {
        super();
        this.loriTimePlugin = plugin;
        this.timeStorage = timeStorage;

        this.log = loriTimePlugin.getLoggerFactory().create(LoriTimePlaceholder.class);
        this.offlinePlayerTime = new HashMap<>();
    }

    @SuppressWarnings("PMD.CyclomaticComplexity")
    @Override
    public String onRequest(final OfflinePlayer player, @NotNull final String params) {
        return switch (params) {
            case "unformatted_onlinetime" -> String.valueOf(getUnformattedOnlineTime(player));
            case "formatted_onlinetime" -> getFormattedOnlineTime(player);
            case "seconds" -> TimeUtil.getSeconds(getUnformattedOnlineTime(player));
            case "minutes" -> TimeUtil.getMinutes(getUnformattedOnlineTime(player));
            case "hours" -> TimeUtil.getHours(getUnformattedOnlineTime(player));
            case "days" -> TimeUtil.getDays(getUnformattedOnlineTime(player));
            case "weeks" -> TimeUtil.getWeeks(getUnformattedOnlineTime(player));
            case "months" -> TimeUtil.getMonths(getUnformattedOnlineTime(player));
            case "years" -> TimeUtil.getYears(getUnformattedOnlineTime(player));
            case "seconds_total" -> TimeUtil.getTotalSeconds(getUnformattedOnlineTime(player));
            case "minutes_total" -> TimeUtil.getTotalMinutes(getUnformattedOnlineTime(player));
            case "hours_total" -> TimeUtil.getTotalHours(getUnformattedOnlineTime(player));
            case "days_total" -> TimeUtil.getTotalDays(getUnformattedOnlineTime(player));
            case "weeks_total" -> TimeUtil.getTotalWeeks(getUnformattedOnlineTime(player));
            case "months_total" -> TimeUtil.getTotalMonths(getUnformattedOnlineTime(player));
            case "years_total" -> TimeUtil.getTotalYears(getUnformattedOnlineTime(player));
            case "afk" -> {
                if (loriTimePlugin.isAfkEnabled()) {
                    final LoriTimePlayer loriTimePlayer = loriTimePlugin.getPlayerConverter().getOnlinePlayer(player.getUniqueId());
                    yield String.valueOf(loriTimePlayer.isAfk());
                }
                yield "Feature not enabled!";
            }
            default -> "Nothing Found";
        };
    }

    private long getUnformattedOnlineTime(final OfflinePlayer player) {
        if (!player.isOnline() && offlinePlayerTime.containsKey(player.getUniqueId())) {
            return offlinePlayerTime.get(player.getUniqueId());
        } else if (offlinePlayerTime.containsKey(player.getUniqueId()) && player.isOnline()) {
            offlinePlayerTime.remove(player.getUniqueId());
        }
        long onlineTime = 0;
        try {
            final OptionalLong optionalLong = timeStorage.getTime(player.getUniqueId());
            if (optionalLong.isPresent()) {
                onlineTime = optionalLong.getAsLong();
            }
        } catch (final StorageException e) {
            log.error("Error while getting the online time placeholder of player " + player.getName(), e);
        }
        if (!player.isOnline()) {
            offlinePlayerTime.put(player.getUniqueId(), onlineTime);
        }
        return onlineTime;
    }

    private String getFormattedOnlineTime(final OfflinePlayer player) {
        return TimeUtil.formatTime(getUnformattedOnlineTime(player), loriTimePlugin.getLocalization());
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
        return loriTimePlugin.getServer().getPluginVersion();
    }
}
