package com.jannik_kuehn.common.storage;

import com.jannik_kuehn.common.api.storage.FileStorage;
import com.jannik_kuehn.common.config.Configuration;
import com.jannik_kuehn.common.exception.StorageException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * File storage provider.
 */
public class FileStorageProvider implements FileStorage {
    /**
     * The {@link Configuration} as storage.
     */
    private final Configuration storage;

    /**
     * The {@link ReadWriteLock}.
     */
    private final ReadWriteLock rwLock;

    /**
     * The closed state.
     */
    private final AtomicBoolean closed;

    /**
     * Creates a new file storage provider.
     *
     * @param file the file
     */
    public FileStorageProvider(final Configuration file) {
        this.storage = file;
        this.rwLock = new ReentrantReadWriteLock();
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public Object read(final String path) throws StorageException {
        Objects.requireNonNull(path);
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            return storage.getObject(path);
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public Map<String, Object> read(final Set<String> paths) throws StorageException {
        Objects.requireNonNull(paths);
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            final Map<String, Object> data = new HashMap<>();
            for (final String key : paths) {
                final Object value = storage.getObject(key);
                if (null != value) {
                    data.put(key, value);
                }
            }
            return data;
        } finally {
            rwLock.readLock().unlock();
        }
    }

    @Override
    public void write(final String path, final Object data) throws StorageException {
        Objects.requireNonNull(path);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            storage.setValue(path, data);
        } catch (final StorageException e) {
            throw new StorageException("failed to write data to path: " + path, e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void write(final String path, final Object data, final boolean overwrite) throws StorageException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(data);
        checkClosed();
        rwLock.writeLock().lock();
        String sameKey = null;
        try {
            final Map<String, Object> all = storage.getAll();
            for (final Map.Entry<String, Object> entry : all.entrySet()) {
                if (entry.getValue().equals(data)) {
                    sameKey = entry.getKey();
                }
            }
        } finally {
            rwLock.writeLock().unlock();
        }

        if (sameKey != null && overwrite) {
            delete(sameKey);
            write(path, data);
        } else {
            write(path, data);
        }
    }

    @Override
    public void writeAll(final Map<String, ?> data) throws StorageException {
        Objects.requireNonNull(data);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            for (final Map.Entry<String, ?> entry : data.entrySet()) {
                storage.setValue(entry.getKey(), entry.getValue());
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void writeAll(final Map<String, ?> data, final boolean overwrite) throws StorageException {
        Objects.requireNonNull(data, "Data map must not be null");
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            for (final Map.Entry<String, ?> entry : data.entrySet()) {
                final String key = entry.getKey();
                final Object value = entry.getValue();

                if (overwrite) {
                    final Map<String, Object> all = storage.getAll();
                    for (final Map.Entry<String, Object> existingEntry : all.entrySet()) {
                        if (existingEntry.getValue().equals(value)) {
                            storage.remove(existingEntry.getKey());
                        }
                    }
                }

                storage.setValue(key, value); // Schreibe den neuen Wert
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void delete(final String path) throws StorageException {
        Objects.requireNonNull(path);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            storage.remove(path);
        } catch (final StorageException e) {
            throw new StorageException("failed to write data to path: " + path, e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Map<String, Object> readAll() throws StorageException {
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            return storage.getAll();
        } finally {
            rwLock.readLock().unlock();
        }
    }

    private void checkClosed() throws StorageException {
        if (closed.get()) {
            throw new StorageException("Storage is already closed!");
        }
    }

    @Override
    public void close() throws StorageException {
        if (closed.get()) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            closed.compareAndSet(false, true);
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
