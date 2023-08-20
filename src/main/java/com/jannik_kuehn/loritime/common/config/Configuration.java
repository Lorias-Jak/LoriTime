package com.jannik_kuehn.loritime.common.config;

import java.util.List;
import java.util.Map;

public abstract class Configuration {
    protected KeyValueStore keyValueStore;

    public Configuration(KeyValueStore keyValueStore) {
        this.keyValueStore = keyValueStore;
    }

    public abstract void setValue(String key, Object value);

    public abstract Object getObject(String key);

    public abstract Object getObject(String key, Object def);

    public List<String> getKeys() {
        return keyValueStore.getKeys();
    }

    public boolean isLoaded() {
        return keyValueStore.isLoaded();
    }

    public Map<String, ?> getAll() {
        return keyValueStore.getAll();
    }

    public void reload() {
        keyValueStore.reload();
    }
}
