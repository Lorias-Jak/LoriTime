package com.jannik_kuehn.common.api.storage;

import com.jannik_kuehn.common.exception.StorageException;

import java.util.Map;
import java.util.Set;

/**
 * Key-value storage abstraction backed by a file configuration document.
 */
public interface FileStorage extends AutoCloseable {

    /**
     * Reads one value by path.
     *
     * @param path value path
     * @return stored value, or null when absent
     * @throws StorageException if reading fails
     */
    Object read(String path) throws StorageException;

    /**
     * Reads multiple values by path.
     *
     * @param paths value paths
     * @return path to value map
     * @throws StorageException if reading fails
     */
    Map<String, Object> read(Set<String> paths) throws StorageException;

    /**
     * Writes a value and replaces existing data.
     *
     * @param path value path
     * @param data value to write
     * @throws StorageException if writing fails
     */
    void write(String path, Object data) throws StorageException;

    /**
     * Writes a value with overwrite control.
     *
     * @param path value path
     * @param data value to write
     * @param overwrite true to replace an existing value
     * @throws StorageException if writing fails
     */
    void write(String path, Object data, boolean overwrite) throws StorageException;

    /**
     * Deletes a value by path.
     *
     * @param path value path
     * @throws StorageException if deletion fails
     */
    void delete(String path) throws StorageException;

    /**
     * Reads all stored values.
     *
     * @return all stored values
     * @throws StorageException if reading fails
     */
    Map<String, ?> readAll() throws StorageException;

    /**
     * Writes all values and replaces existing data.
     *
     * @param data values to write
     * @throws StorageException if writing fails
     */
    void writeAll(Map<String, ?> data) throws StorageException;

    /**
     * Writes all values with overwrite control.
     *
     * @param data values to write
     * @param overwrite true to replace existing values
     * @throws StorageException if writing fails
     */
    void writeAll(Map<String, ?> data, boolean overwrite) throws StorageException;

    /**
     * Closes the storage document.
     *
     * @throws StorageException if closing fails
     */
    @Override
    void close() throws StorageException;
}
