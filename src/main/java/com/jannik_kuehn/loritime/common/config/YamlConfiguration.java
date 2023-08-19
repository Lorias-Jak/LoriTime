package com.jannik_kuehn.loritime.common.config;

public class YamlConfiguration extends Configuration {
    public YamlConfiguration(final String name) {
        super(new YamlKeyValueStore(name));
    }
    @Override
    public void setValue(final String key, final Object value) {
        keyValueStore.set(key, value);
    }

    @Override
    public Object getObject(final String key) {
        return keyValueStore.get(key);
    }

    @Override
    public Object getObject(String key, Object def) {
        return keyValueStore.get(key, def);
    }
}
