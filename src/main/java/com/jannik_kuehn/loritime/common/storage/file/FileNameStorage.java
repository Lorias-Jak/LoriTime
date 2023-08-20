package com.jannik_kuehn.loritime.common.storage.file;

import com.jannik_kuehn.loritime.common.storage.NameStorage;
import com.jannik_kuehn.loritime.common.utils.UuidUtil;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.jannik_kuehn.loritime.api.FileStorageProvider;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

public class FileNameStorage implements NameStorage {

    private final FileStorageProvider storageProvider;

    public FileNameStorage(FileStorageProvider storageProvider) {
        this.storageProvider = Objects.requireNonNull(storageProvider);
    }

    @Override
    public Optional<UUID> getUuid(String playerName) throws StorageException {
        Objects.requireNonNull(playerName);
        Object uuid = storageProvider.read(playerName);
        if (null == uuid) {
            return Optional.empty();
        } else {
            return Optional.of(UUID.fromString(uuid.toString()));
        }
    }

    @Override
    public Optional<String> getName(UUID uniqueId) throws StorageException {
        String value = Objects.requireNonNull(uniqueId).toString();
        return storageProvider.readAll().entrySet().parallelStream()
                .filter(entry -> value.equals(entry.getValue()))
                .findFirst()
                .map(Map.Entry::getKey);
    }

    @Override
    public void setEntry(UUID uuid, String name) throws StorageException {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        storageProvider.write(name, uuid.toString());
    }

    @Override
    public void setEntries(Map<UUID, String> entries) throws StorageException {
        Objects.requireNonNull(entries);
        Map<String, String> data = new HashMap<>();
        for (Map.Entry<UUID, String> entry : entries.entrySet()) {
            String previous = data.put(entry.getValue(), entry.getKey().toString());
            if (previous != null) {
                throw new StorageException("duplicate name: " + entry.getValue());
            }
        }
        storageProvider.writeAll(data);
    }

    @Override
    public Set<String> getEntries() throws StorageException {
        return storageProvider.readAll().keySet();
    }

    @Override
    public void close() throws StorageException {
        storageProvider.close();
    }
}
