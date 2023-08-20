package com.jannik_kuehn.loritime.common.config;

import java.util.List;
import java.util.Map;

public interface KeyValueStore {
    void set(String key, Object value);
    Object get(String key);
    Object get(String key, Object defaultValue);
    Map<String, ?> getAll();
    List<String> getKeys();
    boolean isLoaded();
    void reload();
}
