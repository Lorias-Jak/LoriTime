package com.jannik_kuehn.loritimepaper.placeholder;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;
import com.jannik_kuehn.common.storage.contract.UnifiedStorage;
import com.jannik_kuehn.common.utils.TimeUtil;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import me.clip.placeholderapi.expansion.PlaceholderExpansion;
import org.bukkit.OfflinePlayer;
import org.jetbrains.annotations.NotNull;

import java.util.OptionalLong;

/**
 * PlaceholderAPI expansion for LoriTime online-time placeholders.
 */
@SuppressFBWarnings("HE_INHERITS_EQUALS_USE_HASHCODE")
public class LoriTimePlaceholder extends PlaceholderExpansion {

    /**
     * LoriTime plugin runtime.
     */
    private final LoriTimePlugin loriTimePlugin;

    /**
     * Time cache for placeholder lookups.
     */
    private final PlaceholderTimeCache timeCache;

    /**
     * Logger instance.
     */
    private final WrappedLogger log;

    /**
     * Creates a placeholder expansion backed by storage.
     *
     * @param plugin  LoriTime plugin runtime
     * @param storage storage used for placeholder time lookups
     */
    public LoriTimePlaceholder(final LoriTimePlugin plugin, final UnifiedStorage storage) {
        this(plugin, new StoragePlaceholderTimeCache(plugin, storage));
    }

    /**
     * Creates a placeholder expansion with an explicit time cache.
     *
     * @param plugin    LoriTime plugin runtime
     * @param timeCache placeholder time cache
     */
    public LoriTimePlaceholder(final LoriTimePlugin plugin, final PlaceholderTimeCache timeCache) {
        super();
        this.loriTimePlugin = plugin;
        this.timeCache = timeCache;

        this.log = loriTimePlugin.getLoggerFactory().create(LoriTimePlaceholder.class);
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
                    final TrackedLoriTimePlayer loriTimePlayer = loriTimePlugin.getPlayerConverter().getOnlinePlayer(player.getUniqueId());
                    yield String.valueOf(loriTimePlayer.isAfk());
                }
                yield "Feature not enabled!";
            }
            default -> "Nothing Found";
        };
    }

    private long getUnformattedOnlineTime(final OfflinePlayer player) {
        if (player == null || !player.isOnline()) {
            return 0L;
        }
        final OptionalLong cachedTime = timeCache.getCachedTime(player.getUniqueId());
        timeCache.requestRefresh(player.getUniqueId());
        if (cachedTime.isPresent()) {
            return cachedTime.getAsLong();
        }
        log.debug("Online time placeholder cache miss for player " + player.getUniqueId() + ". Returning 0 while refreshing.");
        return 0L;
    }

    private String getFormattedOnlineTime(final OfflinePlayer player) {
        return TimeUtil.formatTime(getUnformattedOnlineTime(player), loriTimePlugin.getLocalization());
    }

    /**
     * Keeps the expansion registered across PlaceholderAPI reloads.
     *
     * @return true to persist this expansion
     */
    @Override
    public boolean persist() {
        return true; // This is required or else PlaceholderAPI will unregister the Expansion on reload
    }

    /**
     * Returns the PlaceholderAPI identifier.
     *
     * @return placeholder identifier
     */
    @Override
    public @NotNull String getIdentifier() {
        return "LoriTime";
    }

    /**
     * Returns the PlaceholderAPI author.
     *
     * @return author name
     */
    @Override
    public @NotNull String getAuthor() {
        return "Lorias-Jak";
    }

    /**
     * Returns the LoriTime version exposed to PlaceholderAPI.
     *
     * @return plugin version
     */
    @Override
    public @NotNull String getVersion() {
        return loriTimePlugin.getServer().getPluginVersion();
    }
}
