package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.Map;
import java.util.Set;

@SuppressWarnings("PMD.CommentRequired")
public interface FileStorage extends AutoCloseable {

    Object read(String path) throws StorageException;

    Map<String, Object> read(Set<String> paths) throws StorageException;

    void write(String path, Object data) throws StorageException;

    void write(String path, Object data, boolean overwrite) throws StorageException;

    void delete(String path) throws StorageException;

    Map<String, ?> readAll() throws StorageException;

    void writeAll(Map<String, ?> data) throws StorageException;

    void writeAll(Map<String, ?> data, boolean overwrite) throws StorageException;

    @Override
    void close() throws StorageException;
}
