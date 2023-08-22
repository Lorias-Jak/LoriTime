package com.jannik_kuehn.loritime.common.storage;

import com.jannik_kuehn.loritime.common.exception.StorageException;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class AccumulatingTimeStorage implements TimeStorage, TimeAccumulator {

    private final TimeStorage storage;
    private final ConcurrentMap<UUID, Long> onlineSince = new ConcurrentHashMap<>();

    public AccumulatingTimeStorage(TimeStorage timeStorage) {
        this.storage = Objects.requireNonNull(timeStorage);
    }

    @Override
    public OptionalLong getTime(UUID uniqueId) throws StorageException {
        if (onlineSince.containsKey(uniqueId)) {
            long accumulatedTime = (System.currentTimeMillis() - onlineSince.get(uniqueId)) / 1000L;
            long storedTime = storage.getTime(uniqueId).orElse(0);
            return OptionalLong.of(accumulatedTime + storedTime);
        } else {
            return storage.getTime(uniqueId);
        }
    }

    @Override
    public void addTime(UUID uuid, long additionalTime) throws StorageException {
        Long present = onlineSince.computeIfPresent(uuid, (key, value) -> value - additionalTime * 1000);
        if (null == present) {
            // no entry for uuid present, directly writing to storage
            this.storage.addTime(uuid, additionalTime);
        }
    }

    @Override
    public void addTimes(Map<UUID, Long> additionalTimes) throws StorageException {
        Map<UUID, Long> directWrite = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
            Long present = onlineSince.computeIfPresent(entry.getKey(), (key, value) -> value - entry.getValue() * 1000);
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
    public void startAccumulating(UUID uuid, long when) throws StorageException {
        Long from = onlineSince.put(uuid, when);
        if (null != from) {
            long previousOnlineTime = (when - from) / 1000;
            storage.addTime(uuid, previousOnlineTime);
        }
    }

    @Override
    public void stopAccumulatingAndSaveOnlineTime(UUID uuid, long when) throws StorageException {
        if (onlineSince.containsKey(uuid)) {
            Long from = onlineSince.remove(uuid);
            if (null != from) {
                long currentOnlineTime = (when - from) / 1000;
                storage.addTime(uuid, currentOnlineTime);
            } // else already stopped concurrently
        }
    }

    @Override
    public void flushOnlineTimeCache() throws StorageException {
        if (onlineSince.isEmpty()) {
            return;
        }
        final Map<UUID, Long> onlineTime = new HashMap<>();
        final long now = System.currentTimeMillis();
        onlineSince.keySet().forEach(uuid -> {
            Long from = onlineSince.replace(uuid, now);
            if (from != null) { // protect from concurrent change
                onlineTime.put(uuid, (now - from) / 1000);
            }
        });
        storage.addTimes(onlineTime);
    }

    @Override
    public void close() throws StorageException {
        try {
            if (!onlineSince.isEmpty()) {
                final Map<UUID, Long> onlineTime = new HashMap<>();
                final long now = System.currentTimeMillis();
                new HashSet<>(onlineSince.keySet()).forEach(uuid -> {
                    Long from = onlineSince.remove(uuid);
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
