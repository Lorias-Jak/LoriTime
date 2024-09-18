package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.Map;
import java.util.OptionalLong;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public interface TimeStorage extends AutoCloseable {

    OptionalLong getTime(UUID uniqueId) throws StorageException;

    void addTime(UUID uuid, long additionalTime) throws StorageException;

    void addTimes(Map<UUID, Long> additionalTimes) throws StorageException;

    Map<String, ?> getAllTimeEntries() throws StorageException;

    @Override
    void close() throws StorageException;
}
