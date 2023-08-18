package com.jannik_kuehn.loritime.common.config;

import java.util.List;

public interface KeyValueStore {
    void set(String key, Object value);
    Object get(String key);
    Object get(String key, Object defaultValue);
    List<String> getKeys();
    boolean isLoaded();
}
