package com.jannik_kuehn.common.api;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoggerFactory;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The {@link LoriTimePlayerConverter} holds a cache of {@link LoriTimePlayer} instances and provides methods to get.
 * If the player is not cached yet, it will be created and cached.
 */
public class LoriTimePlayerConverter {
    /**
     * The player cache.
     */
    private final Map<UUID, LoriTimePlayer> playerCache;

    /**
     * The {@link LoriTimeLogger} instance.
     */
    private final LoriTimeLogger log;

    /**
     * The {@link LoriTimePlugin} instance.
     */
    private final LoriTimePlugin loriTimePlugin;

    /**
     * Creates a new {@link LoriTimePlayerConverter} instance.
     *
     * @param loggerFactory  The {@link LoggerFactory} instance.
     * @param loriTimePlugin The {@link LoriTimePlugin} instance.
     */
    public LoriTimePlayerConverter(final LoggerFactory loggerFactory, final LoriTimePlugin loriTimePlugin) {
        this.log = loggerFactory.create(LoriTimePlayerConverter.class);
        this.loriTimePlugin = loriTimePlugin;
        this.playerCache = new ConcurrentHashMap<>();
    }

    /**
     * Gets a {@link LoriTimePlayer} instance for the given {@link UUID}.
     * If the player is not cached yet, it will be created and cached.
     * If the player is not online, the player will be created offline and cached too.
     *
     * @param uuid The {@link UUID} of the player.
     * @return The {@link LoriTimePlayer} instance.
     */
    public LoriTimePlayer getOnlinePlayer(final UUID uuid) {
        return playerCache.computeIfAbsent(uuid, key -> {
            final Optional<CommonSender> optionalPlayer = loriTimePlugin.getServer().getPlayer(uuid);
            if (optionalPlayer.isEmpty()) {
                return getOfflinePlayer(uuid);
            }
            final LoriTimePlayer player = new LoriTimePlayer(uuid, optionalPlayer.get().getName());
            log.debug("Created new LoriTimePlayer for UUID " + uuid);
            return player;
        });
    }

    /**
     * Gets a {@link LoriTimePlayer} instance for the given player.
     * If the player is not cached yet, it will be created and cached.
     *
     * @param player The player.
     * @return The {@link LoriTimePlayer} instance.
     */
    public LoriTimePlayer getOnlinePlayer(final LoriTimePlayer player) {
        return playerCache.computeIfAbsent(player.getUniqueId(), key -> {
            log.debug("Created new LoriTimePlayer for UUID " + player.getUniqueId());
            return player;
        });
    }

    /**
     * Gets a {@link LoriTimePlayer} instance for the given {@link UUID}. If the player is not cached yet,
     *
     * @param uuid The {@link UUID} of the player.
     * @return The {@link LoriTimePlayer} instance.
     */
    public LoriTimePlayer getOfflinePlayer(final UUID uuid) {
        try {
            final Optional<String> optionalName = loriTimePlugin.getNameStorage().getName(uuid);
            if (optionalName.isEmpty()) {
                log.warn("Could not get name for UUID " + uuid);
                return null;
            }
            return new LoriTimePlayer(uuid, optionalName.get());
        } catch (final StorageException ex) {
            log.error("Could not get name for UUID " + uuid, ex);
            return null;
        }
    }

    /**
     * Gets a {@link LoriTimePlayer} instance for the given player.
     *
     * @return The {@link LoriTimePlayer} instance.
     */
    public List<LoriTimePlayer> getOnlinePlayers() {
        return List.copyOf(playerCache.values());
    }

    /**
     * Removes a player from the cache.
     *
     * @param uuid The {@link UUID} of the player.
     */
    public void removePlayerFromCache(final UUID uuid) {
        playerCache.remove(uuid);
    }
}
