package com.jannik_kuehn.loritime.api.storage;

import com.jannik_kuehn.loritime.common.exception.StorageException;

import java.util.UUID;

public interface TimeAccumulator extends AutoCloseable {

    void startAccumulating(UUID uuid, long when) throws StorageException;

    void stopAccumulatingAndSaveOnlineTime(UUID uuid, long when) throws StorageException;

    void flushOnlineTimeCache() throws StorageException;

    @Override
    void close() throws StorageException;
}
