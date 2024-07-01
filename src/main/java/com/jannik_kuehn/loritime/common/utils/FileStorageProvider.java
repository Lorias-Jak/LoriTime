package com.jannik_kuehn.loritime.common.utils;

import com.jannik_kuehn.loritime.api.storage.FileStorage;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.Configuration;
import com.jannik_kuehn.loritime.common.exception.StorageException;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class FileStorageProvider implements FileStorage {
    private final LoriTimePlugin plugin;
    private final Configuration storage;
    private final ReadWriteLock rwLock;
    private final AtomicBoolean closed;

    public FileStorageProvider(LoriTimePlugin plugin, Configuration file) {
        this.plugin = plugin;
        this.storage = file;
        this.rwLock = new ReentrantReadWriteLock();
        this.closed = new AtomicBoolean(false);
    }

    @Override
    public Object read(String path) throws StorageException {
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
    public Map<String, ?> read(Set<String> paths) throws StorageException {
        Objects.requireNonNull(paths);
        checkClosed();
        rwLock.readLock().lock();
        try {
            checkClosed();
            Map<String, Object> data = new HashMap<>();
            for (String key : paths) {
                Object value = storage.getObject(key);
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
    public void write(String path, Object data) throws StorageException {
        Objects.requireNonNull(path);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            storage.setValue(path, data);
        } catch (StorageException e) {
            throw new StorageException("failed to write data to path: " + path, e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void write(String path, Object data, boolean overwrite) throws StorageException {
        Objects.requireNonNull(path);
        Objects.requireNonNull(data);
        checkClosed();
        rwLock.writeLock().lock();
        String sameKey = null;
        try {
            Map<String, Object> all = (Map<String, Object>) storage.getAll();
            Set<String> set = all.keySet();
            for (String value : set) {
                if (all.get(value).equals(data)) {
                    sameKey = value;
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
    public void writeAll(Map<String, ?> data) throws StorageException {
        Objects.requireNonNull(data);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            for (Map.Entry<String, ?> entry : data.entrySet()) {
                storage.setValue(entry.getKey(), entry.getValue());
            }
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public void writeAll(Map<String, ?> data, boolean overwrite) throws StorageException {

    }

    @Override
    public void delete(String path) throws StorageException {
        Objects.requireNonNull(path);
        checkClosed();
        rwLock.writeLock().lock();
        try {
            checkClosed();
            storage.remove(path);
        } catch (StorageException e) {
            throw new StorageException("failed to write data to path: " + path, e);
        } finally {
            rwLock.writeLock().unlock();
        }
    }

    @Override
    public Map<String, ?> readAll() throws StorageException {
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
            throw new StorageException("closed");
        }
    }

    @Override
    public void close() throws StorageException {
        if (closed.get()) {
            return;
        }
        rwLock.writeLock().lock();
        try {
            closed.compareAndSet(false, true);// nothing to do on close
        } finally {
            rwLock.writeLock().unlock();
        }
    }
}
