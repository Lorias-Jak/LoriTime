package com.jannik_kuehn.loritime.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public abstract class Configuration {
    protected KeyValueStore keyValueStore;

    public Configuration(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    public abstract void setValue(String key, Object value);

    public abstract String getString(String key);

    public abstract String getString(String key, String def);

    public abstract int getInt(String key);

    public abstract int getInt(String key, int def);

    public abstract long getLong(String key);

    public abstract long getLong(String key, long def);

    public abstract ArrayList<?> getArrayList(String key);

    public abstract Object getObject(String key);

    public abstract Object getObject(String key, Object def);

    public abstract List<String> getKeys();

    public abstract boolean isLoaded();

    public abstract Map<String, ?> getAll();

    public abstract void reload();

}
