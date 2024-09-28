package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

@SuppressWarnings("PMD.CommentRequired")
public interface NameStorage extends AutoCloseable {

    Optional<UUID> getUuid(String playerName) throws StorageException;

    Optional<String> getName(UUID uniqueId) throws StorageException;

    void setEntry(UUID uniqueId, String name) throws StorageException;

    void setEntry(UUID uniqueId, String name, boolean override) throws StorageException;

    void setEntries(Map<UUID, String> entries) throws StorageException;

    Set<String> getNameEntries() throws StorageException;

    @Override
    void close() throws StorageException;
}
