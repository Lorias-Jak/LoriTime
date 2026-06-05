package com.jannik_kuehn.loritimepaper.placeholder;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.storage.contract.UnifiedStorage;

import java.util.Map;
import java.util.OptionalLong;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Async-refreshing placeholder cache for canonical storage instances.
 */
public final class StoragePlaceholderTimeCache implements PlaceholderTimeCache {

    /**
     * Backing LoriTime plugin.
     */
    private final LoriTimePlugin plugin;

    /**
     * Storage used from asynchronous refresh tasks.
     */
    private final UnifiedStorage storage;

    /**
     * Logger for refresh failures.
     */
    private final WrappedLogger log;

    /**
     * Cached time values by player UUID.
     */
    private final Map<UUID, Long> cachedTimes;

    /**
     * Player UUIDs with a currently scheduled refresh.
     */
    private final Set<UUID> refreshesInProgress;

    /**
     * Creates a cache backed by asynchronous storage refreshes.
     *
     * @param plugin  the LoriTime plugin
     * @param storage storage to refresh from
     */
    public StoragePlaceholderTimeCache(final LoriTimePlugin plugin, final UnifiedStorage storage) {
        this.plugin = plugin;
        this.storage = storage;
        this.log = plugin.getLoggerFactory().create(StoragePlaceholderTimeCache.class);
        this.cachedTimes = new ConcurrentHashMap<>();
        this.refreshesInProgress = ConcurrentHashMap.newKeySet();
    }

    @Override
    public OptionalLong getCachedTime(final UUID uniqueId) {
        final Long value = cachedTimes.get(uniqueId);
        return value == null ? OptionalLong.empty() : OptionalLong.of(value);
    }

    @Override
    public void requestRefresh(final UUID uniqueId) {
        if (!refreshesInProgress.add(uniqueId)) {
            return;
        }
        plugin.getScheduler().runAsyncOnce(() -> {
            try {
                final OptionalLong time = storage.getTime(uniqueId);
                cachedTimes.put(uniqueId, time.orElse(0L));
            } catch (final StorageException e) {
                log.error("Error while refreshing the online time placeholder cache for player " + uniqueId, e);
            } finally {
                refreshesInProgress.remove(uniqueId);
            }
        });
    }
}
