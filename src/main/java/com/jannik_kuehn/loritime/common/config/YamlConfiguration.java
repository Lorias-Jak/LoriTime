package com.jannik_kuehn.loritime.common.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class YamlConfiguration extends Configuration {

    public YamlConfiguration(final String name) {
        super(new YamlKeyValueStore(name));
    }
    @Override
    public void setValue(final String key, final Object value) {
        keyValueStore.set(key, value);
    }

    @Override
    public String getString(String key) {
        return (String) keyValueStore.get(key);
    }

    @Override
    public String getString(String key, String def) {
        return (String) keyValueStore.get(key, def);
    }

    @Override
    public int getInt(String key) {
        return (int) keyValueStore.get(key);
    }

    @Override
    public int getInt(String key, int def) {
        return (int) keyValueStore.get(key, def);
    }

    @Override
    public long getLong(String key) {
        return (long) keyValueStore.get(key);
    }

    @Override
    public long getLong(String key, long def) {
        return (long) keyValueStore.get(key, def);
    }

    @Override
    public boolean getBoolean(String key) {
        return (boolean) keyValueStore.get(key);
    }

    @Override
    public boolean getBoolean(String key, boolean def) {
        return (boolean) keyValueStore.get(key, def);
    }

    @Override
    public ArrayList<?> getArrayList(String key) {
        return (ArrayList<?>) keyValueStore.get(key);
    }

    @Override
    public Object getObject(final String key) {
        return keyValueStore.get(key);
    }

    @Override
    public Object getObject(String key, Object def) {
        return keyValueStore.get(key, def);
    }

    @Override
    public List<String> getKeys() {
        return keyValueStore.getKeys();
    }

    @Override
    public boolean isLoaded() {
        return keyValueStore.isLoaded();
    }

    @Override
    public Map<String, ?> getAll() {
        return keyValueStore.getAll();
    }

    @Override
    public void reload() {
        keyValueStore.reload();
    }

    @Override
    public void remove(String key) {
        keyValueStore.remove(key);
    }
}
