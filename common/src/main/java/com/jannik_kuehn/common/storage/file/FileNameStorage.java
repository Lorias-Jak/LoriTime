package com.jannik_kuehn.common.storage.file;

import com.jannik_kuehn.common.api.storage.FileStorage;
import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.exception.StorageException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

/**
 * Name storage implementation using a file storage provider.
 */
public class FileNameStorage implements NameStorage {
    /**
     * The {@link FileStorage}.
     */
    private final FileStorage storageProvider;

    /**
     * Creates a new file name storage.
     *
     * @param storageProvider the {@link FileStorage} provider
     */
    public FileNameStorage(final FileStorage storageProvider) {
        this.storageProvider = Objects.requireNonNull(storageProvider);
    }

    @Override
    public Optional<UUID> getUuid(final String playerName) throws StorageException {
        Objects.requireNonNull(playerName);
        final Object uuid = storageProvider.read(playerName);
        if (null == uuid) {
            return Optional.empty();
        } else {
            return Optional.of(UUID.fromString(uuid.toString()));
        }
    }

    @Override
    public Optional<String> getName(final UUID uniqueId) throws StorageException {
        final String value = Objects.requireNonNull(uniqueId).toString();
        return storageProvider.readAll().entrySet().parallelStream()
                .filter(entry -> value.equals(entry.getValue()))
                .findFirst()
                .map(Map.Entry::getKey);
    }

    @Override
    public void setEntry(final UUID uuid, final String name) throws StorageException {
        Objects.requireNonNull(uuid);
        Objects.requireNonNull(name);
        storageProvider.write(name, uuid.toString());
    }

    @Override
    public void setEntry(final UUID uniqueId, final String name, final boolean override) throws StorageException {
        Objects.requireNonNull(uniqueId);
        Objects.requireNonNull(name);
        storageProvider.write(name, uniqueId.toString(), override);
    }

    @Override
    public void setEntries(final Map<UUID, String> entries) throws StorageException {
        Objects.requireNonNull(entries);
        final Map<String, String> data = new HashMap<>();
        for (final Map.Entry<UUID, String> entry : entries.entrySet()) {
            final String previous = data.put(entry.getValue(), entry.getKey().toString());
            if (previous != null) {
                throw new StorageException("duplicate name: " + entry.getValue());
            }
        }
        storageProvider.writeAll(data);
    }

    @Override
    public Set<String> getNameEntries() throws StorageException {
        return storageProvider.readAll().keySet();
    }

    @Override
    public void close() throws StorageException {
        storageProvider.close();
    }
}
