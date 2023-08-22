package com.jannik_kuehn.loritime.common.storage;

import com.jannik_kuehn.loritime.common.exception.StorageException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public interface NameStorage extends AutoCloseable {

    Optional<UUID> getUuid(String playerName) throws StorageException;

    Optional<String> getName(UUID uniqueId) throws StorageException;

    void setEntry(UUID uniqueId, String name) throws StorageException;

    void setEntries(Map<UUID, String> entries) throws StorageException;

    Set<String> getNameEntries() throws StorageException;

    @Override
    void close() throws StorageException;
}
