package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.exception.StorageException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@SuppressWarnings("PMD.CommentRequired")
public class AccumulatingTimeStorage implements TimeStorage, TimeAccumulator {

    private final LoriTimeLogger log;

    private final TimeStorage storage;

    private final ConcurrentMap<UUID, Long> onlineSince = new ConcurrentHashMap<>();

    public AccumulatingTimeStorage(final LoriTimeLogger log, final TimeStorage timeStorage) {
        this.log = log;
        this.storage = Objects.requireNonNull(timeStorage);
    }

    @Override
    public OptionalLong getTime(final UUID uniqueId) throws StorageException {
        if (onlineSince.containsKey(uniqueId)) {
            final long accumulatedTime = (System.currentTimeMillis() - onlineSince.get(uniqueId)) / 1000L;
            final long storedTime = storage.getTime(uniqueId).orElse(0);
            return OptionalLong.of(accumulatedTime + storedTime);
        } else {
            return storage.getTime(uniqueId);
        }
    }

    @Override
    public void addTime(final UUID uuid, final long additionalTime) throws StorageException {
        final Long present = onlineSince.computeIfPresent(uuid, (key, value) -> value - additionalTime * 1000);
        if (null == present) {
            // no entry for uuid present, directly writing to storage
            this.storage.addTime(uuid, additionalTime);
        }
    }

    @Override
    public void addTimes(final Map<UUID, Long> additionalTimes) throws StorageException {
        final Map<UUID, Long> directWrite = new HashMap<>();
        for (final Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
            final Long present = onlineSince.computeIfPresent(entry.getKey(), (key, value) -> value - entry.getValue() * 1000);
            if (null == present) {
                // no entry for uuid present, directly writing to storage
                directWrite.put(entry.getKey(), entry.getValue());
            }
        }
        storage.addTimes(directWrite);
    }

    @Override
    public Map<String, ?> getAllTimeEntries() throws StorageException {
        return storage.getAllTimeEntries();
    }

    @Override
    public void startAccumulating(final UUID uuid, final long when) throws StorageException {
        final Long from = onlineSince.put(uuid, when);
        if (null != from) {
            final long previousOnlineTime = (when - from) / 1000;
            storage.addTime(uuid, previousOnlineTime);
        }
    }

    @Override
    public void stopAccumulatingAndSaveOnlineTime(final UUID uuid, final long when) throws StorageException {
        if (onlineSince.containsKey(uuid)) {
            final Long from = onlineSince.remove(uuid);
            if (null != from) {
                final long currentOnlineTime = (when - from) / 1000;
                storage.addTime(uuid, currentOnlineTime);
            } // else already stopped concurrently
        }
    }

    @Override
    public void flushOnlineTimeCache() throws StorageException {
        if (onlineSince.isEmpty()) {
            return;
        }
        log.debug("Flushing online time cache");
        final Map<UUID, Long> onlineTime = new HashMap<>();
        final long now = System.currentTimeMillis();
        onlineSince.keySet().forEach(uuid -> {
            final Long from = onlineSince.replace(uuid, now);
            if (from != null) {
                onlineTime.put(uuid, (now - from) / 1000);
            }
        });
        storage.addTimes(onlineTime);
    }

    @Override
    @SuppressWarnings("PMD.UseTryWithResources")
    public void close() throws StorageException {
        try {
            if (!onlineSince.isEmpty()) {
                final Map<UUID, Long> onlineTime = new HashMap<>();
                final long now = System.currentTimeMillis();
                new HashSet<>(onlineSince.keySet()).forEach(uuid -> {
                    final Long from = onlineSince.remove(uuid);
                    if (from != null) { // protect from concurrent change
                        onlineTime.put(uuid, (now - from) / 1000);
                    }
                });
                storage.addTimes(onlineTime);
            }
        } finally {
            this.storage.close();
        }
    }
}
