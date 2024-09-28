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

public class LoriTimePlayerConverter {
    private final Map<UUID, LoriTimePlayer> playerCache;

    private final LoriTimeLogger log;

    private final LoriTimePlugin loriTimePlugin;

    public LoriTimePlayerConverter(final LoggerFactory loggerFactory, final LoriTimePlugin loriTimePlugin) {
        this.log = loggerFactory.create(LoriTimePlayerConverter.class);
        this.loriTimePlugin = loriTimePlugin;
        this.playerCache = new ConcurrentHashMap<>();
    }

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

    public LoriTimePlayer getOnlinePlayer(final LoriTimePlayer player) {
        return playerCache.computeIfAbsent(player.getUniqueId(), key -> {
            log.debug("Created new LoriTimePlayer for UUID " + player.getUniqueId());
            return player;
        });
    }

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

    public List<LoriTimePlayer> getOnlinePlayers() {
        return List.copyOf(playerCache.values());
    }

    public void removePlayerFromCache(final UUID uuid) {
        playerCache.remove(uuid);
    }
}
