package com.jannik_kuehn.loritime.common.config;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

public interface KeyValueStore {
    void set(String key, Object value);
    Object get(String key);
    Object get(String key, Object defaultValue);
    Map<String, ?> getAll();
    List<String> getKeys();
    Set<Map.Entry<String, Object>> entrySet();
    boolean isLoaded();
    void reload();
}
