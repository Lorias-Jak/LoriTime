package com.jannik_kuehn.common.config;

import java.util.List;
import java.util.Map;

@SuppressWarnings("PMD.CommentRequired")
public class YamlConfiguration extends Configuration {

    public YamlConfiguration(final String name) {
        super(new YamlKeyValueStore(name));
    }

    @Override
    public void setValue(final String key, final Object value) {
        keyValueStore.set(key, value);
    }

    @Override
    public void setTemporaryValue(final String key, final Object value) {
        keyValueStore.setTemporary(key, value);
    }

    @Override
    public String getString(final String key) {
        return (String) keyValueStore.get(key);
    }

    @Override
    public String getString(final String key, final String def) {
        return (String) keyValueStore.get(key, def);
    }

    @Override
    public int getInt(final String key) {
        return (int) keyValueStore.get(key);
    }

    @Override
    public int getInt(final String key, final int def) {
        return (int) keyValueStore.get(key, def);
    }

    @Override
    public long getLong(final String key) {
        return (long) keyValueStore.get(key);
    }

    @Override
    public long getLong(final String key, final long def) {
        return (long) keyValueStore.get(key, def);
    }

    @Override
    public boolean getBoolean(final String key) {
        return (boolean) keyValueStore.get(key);
    }

    @Override
    public boolean getBoolean(final String key, final boolean def) {
        return (boolean) keyValueStore.get(key, def);
    }

    @Override
    public List<?> getArrayList(final String key) {
        return (List<?>) keyValueStore.get(key);
    }

    @Override
    public Object getObject(final String key) {
        return keyValueStore.get(key);
    }

    @Override
    public Object getObject(final String key, final Object def) {
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
    public Map<String, Object> getAll() {
        return keyValueStore.getAll();
    }

    @Override
    public void reload() {
        keyValueStore.reload();
    }

    @Override
    public void remove(final String key) {
        keyValueStore.remove(key);
    }
}
