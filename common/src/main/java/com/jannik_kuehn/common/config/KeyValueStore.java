package com.jannik_kuehn.common.config;

import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Legacy flat key-value storage contract for YAML-backed data files.
 */
public interface KeyValueStore {
    /**
     * Stores a value and persists it.
     *
     * @param key   flat key
     * @param value value to store
     */
    void set(String key, Object value);

    /**
     * Stores a value in memory without immediately persisting it.
     *
     * @param key   flat key
     * @param value value to store
     */
    void setTemporary(String key, Object value);

    /**
     * Reads a value.
     *
     * @param key flat key
     * @return stored value, or null
     */
    Object get(String key);

    /**
     * Reads a value.
     *
     * @param key          flat key
     * @param defaultValue fallback value
     * @return stored value, or fallback
     */
    Object get(String key, Object defaultValue);

    /**
     * Returns all stored values.
     *
     * @return stored values
     */
    Map<String, Object> getAll();

    /**
     * Returns stored keys.
     *
     * @return keys
     */
    List<String> getKeys();

    /**
     * Returns stored entries.
     *
     * @return entries
     */
    Set<Map.Entry<String, Object>> entrySet();

    /**
     * Checks whether the backing file loaded successfully.
     *
     * @return true when loaded
     */
    boolean isLoaded();

    /**
     * Reloads values from disk.
     */
    void reload();

    /**
     * Removes a value and persists the change.
     *
     * @param key flat key
     */
    void remove(String key);
}
