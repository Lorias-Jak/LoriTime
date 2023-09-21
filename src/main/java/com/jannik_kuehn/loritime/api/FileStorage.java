package com.jannik_kuehn.loritime.api;

import com.jannik_kuehn.loritime.common.exception.StorageException;

import java.util.Map;
import java.util.Set;

public interface FileStorage extends AutoCloseable {

    Object read(String path) throws StorageException;

    Map<String, ?> read(Set<String> paths) throws StorageException;

    void write(String path, Object data) throws StorageException;

    void delete(String path) throws StorageException;

    Map<String, ?> readAll() throws StorageException;

    void writeAll(Map<String, ?> data) throws StorageException;

    void close() throws StorageException;
}
