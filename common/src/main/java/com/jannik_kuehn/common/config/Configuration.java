package com.jannik_kuehn.common.config;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Base abstraction for plugin configuration files.
 */
public abstract class Configuration {
    /**
     * Optional backing key-value store for legacy implementations.
     */
    protected KeyValueStore keyValueStore;

    /**
     * Creates a new configuration wrapper.
     *
     * @param keyValueStore optional backing key-value store
     */
    public Configuration(final KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    /**
     * Stores a value and persists the configuration.
     *
     * @param key   dot-path key
     * @param value value to store
     */
    public abstract void setValue(String key, Object value);

    /**
     * Stores a value in memory without immediately persisting it.
     *
     * @param key   dot-path key
     * @param value value to store
     */
    public abstract void setTemporaryValue(String key, Object value);

    /**
     * Reads a string value.
     *
     * @param key dot-path key
     * @return configured value, or null
     */
    public abstract String getString(String key);

    /**
     * Reads a string value.
     *
     * @param key dot-path key
     * @param def fallback value
     * @return configured value, or fallback
     */
    public abstract String getString(String key, String def);

    /**
     * Reads an integer value.
     *
     * @param key dot-path key
     * @return configured value, or zero
     */
    public abstract int getInt(String key);

    /**
     * Reads an integer value.
     *
     * @param key dot-path key
     * @param def fallback value
     * @return configured value, or fallback
     */
    public abstract int getInt(String key, int def);

    /**
     * Reads a long value.
     *
     * @param key dot-path key
     * @return configured value, or zero
     */
    public abstract long getLong(String key);

    /**
     * Reads a long value.
     *
     * @param key dot-path key
     * @param def fallback value
     * @return configured value, or fallback
     */
    public abstract long getLong(String key, long def);

    /**
     * Reads a boolean value.
     *
     * @param key dot-path key
     * @return configured value, or false
     */
    public abstract boolean getBoolean(String key);

    /**
     * Reads a boolean value.
     *
     * @param key dot-path key
     * @param def fallback value
     * @return configured value, or fallback
     */
    public abstract boolean getBoolean(String key, boolean def);

    /**
     * Reads a list value.
     *
     * @param key dot-path key
     * @return configured list, or an empty list
     */
    public abstract List<?> getArrayList(String key);

    /**
     * Reads a raw value.
     *
     * @param key dot-path key
     * @return configured value, or null
     */
    public abstract Object getObject(String key);

    /**
     * Reads a raw value.
     *
     * @param key dot-path key
     * @param def fallback value
     * @return configured value, or fallback
     */
    public abstract Object getObject(String key, Object def);

    /**
     * Returns flattened dot-path keys.
     *
     * @return flattened keys
     */
    public abstract List<String> getKeys();

    /**
     * Checks whether the configuration loaded successfully.
     *
     * @return true when loaded
     */
    public abstract boolean isLoaded();

    /**
     * Returns flattened dot-path values for compatibility.
     *
     * @return flattened values
     */
    public abstract Map<String, Object> getAll();

    /**
     * Returns structured nested configuration values.
     *
     * @return structured values
     */
    public abstract Map<String, Object> getStructuredValues();

    /**
     * Returns the root configuration section.
     *
     * @return root section
     */
    public abstract ConfigSection getRootSection();

    /**
     * Returns a nested section.
     *
     * @param key dot-path key
     * @return section when present
     */
    public abstract Optional<ConfigSection> getSection(String key);

    /**
     * Reloads the configuration from disk.
     */
    public abstract void reload();

    /**
     * Removes a value and persists the configuration.
     *
     * @param key dot-path key
     */
    public abstract void remove(String key);

    /**
     * Checks whether a flattened key exists.
     *
     * @param key dot-path key
     * @return true when present
     */
    public boolean containsKey(final String key) {
        return getKeys().contains(key);
    }

    /**
     * Returns the optional backing key-value store.
     *
     * @return backing key-value store
     */
    protected KeyValueStore getKeyValueStore() {
        return keyValueStore;
    }

}
