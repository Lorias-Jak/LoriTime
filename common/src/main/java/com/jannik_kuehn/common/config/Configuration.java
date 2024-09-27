package com.jannik_kuehn.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Configuration {
    protected KeyValueStore keyValueStore;

    public Configuration(final KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    public abstract void setValue(String key, Object value);

    public abstract void setTemporaryValue(String key, Object value);

    public abstract String getString(String key);

    public abstract String getString(String key, String def);

    public abstract int getInt(String key);

    public abstract int getInt(String key, int def);

    public abstract long getLong(String key);

    public abstract long getLong(String key, long def);

    public abstract boolean getBoolean(String key);

    public abstract boolean getBoolean(String key, boolean def);

    public abstract ArrayList<?> getArrayList(String key);

    public abstract Object getObject(String key);

    public abstract Object getObject(String key, Object def);

    public abstract List<String> getKeys();

    public abstract boolean isLoaded();

    public abstract Map<String, ?> getAll();

    public abstract void reload();

    public abstract void remove(String key);

    public boolean containsKey(final String key) {
        return getKeys().contains(key);
    }

    protected KeyValueStore getKeyValueStore() {
        return keyValueStore;
    }

}
