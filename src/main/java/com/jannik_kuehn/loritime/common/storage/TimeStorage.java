package com.jannik_kuehn.loritime.common.storage;

import com.jannik_kuehn.loritime.common.exception.StorageException;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

public interface TimeStorage extends AutoCloseable {

    OptionalLong getTime(UUID uniqueId) throws StorageException;

    void addTime(UUID uuid, long additionalTime) throws StorageException;

    void addTimes(Map<UUID, Long> additionalTimes) throws StorageException;

    @Override
    void close() throws StorageException;
}
