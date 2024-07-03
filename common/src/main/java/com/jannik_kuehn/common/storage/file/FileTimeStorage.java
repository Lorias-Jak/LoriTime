package com.jannik_kuehn.common.storage.file;

import com.jannik_kuehn.common.api.storage.FileStorage;
import com.jannik_kuehn.common.api.storage.TimeStorage;
import com.jannik_kuehn.common.exception.StorageException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.OptionalLong;
import java.util.UUID;

public class FileTimeStorage implements TimeStorage {
    private final FileStorage storageProvider;

    public FileTimeStorage(FileStorage storageProvider) {
        this.storageProvider = Objects.requireNonNull(storageProvider);
    }

    @Override
    public OptionalLong getTime(UUID uniqueId) throws StorageException {
        Objects.requireNonNull(uniqueId);
        String path = uniqueId.toString();
        return internalReadOnlineTime(path);
    }

    @Override
    public void addTime(UUID uuid, long additionalTime) throws StorageException {
        Objects.requireNonNull(uuid);
        String path = uuid.toString();
        long amount = internalReadOnlineTime(path).orElse(0) + additionalTime;
        storageProvider.write(path, amount);
    }

    private OptionalLong internalReadOnlineTime(String path) throws StorageException {
        Object result = storageProvider.read(path);
        if (result instanceof Long || result instanceof Integer) {
            return OptionalLong.of(((Number) result).longValue());
        } else {
            return OptionalLong.empty();
        }
    }

    @Override
    public void addTimes(Map<UUID, Long> additionalTimes) throws StorageException {
        Objects.requireNonNull(additionalTimes);
        Map<String, Long> writeData = new HashMap<>();
        for (Map.Entry<UUID, Long> entry : additionalTimes.entrySet()) {
            writeData.put(entry.getKey().toString(), entry.getValue());
        }
        Map<String, ?> storedTime = storageProvider.read(writeData.keySet());
        for (Map.Entry<String, ?> entry : storedTime.entrySet()) {
            Object result = entry.getValue();
            if (result instanceof Long || result instanceof Integer) {
                long time = ((Number) result).longValue();
                writeData.compute(entry.getKey(), (key, value) -> null == value ? null : time + value);
            }
        }
        storageProvider.writeAll(writeData);
    }

    @Override
    public Map<String, ?> getAllTimeEntries() throws StorageException {
        return storageProvider.readAll();
    }

    @Override
    public void close() throws StorageException {
        storageProvider.close();
    }
}
