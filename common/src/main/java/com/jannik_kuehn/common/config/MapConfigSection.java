package com.jannik_kuehn.common.config;

import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Map-backed implementation of a configuration section.
 */
@SuppressWarnings("PMD.TooManyMethods")
final class MapConfigSection implements ConfigSection {
    /**
     * Backing document.
     */
    private final StructuredConfigurationDocument document;

    /**
     * Absolute section path.
     */
    private final String path;

    /* default */ MapConfigSection(final StructuredConfigurationDocument document, final String path) {
        this.document = document;
        this.path = path == null ? "" : path;
    }

    @Override
    public String getPath() {
        return path;
    }

    @Override
    public Set<String> getKeys() {
        return getKeys(false);
    }

    @Override
    public Set<String> getKeys(final boolean recursive) {
        final Set<String> keys = new LinkedHashSet<>();
        collectKeys("", document.sectionMap(path), recursive, keys);
        return keys;
    }

    @Override
    public boolean contains(final String relativePath) {
        return document.contains(resolve(relativePath));
    }

    @Override
    public Optional<ConfigSection> getSection(final String relativePath) {
        return document.getSection(resolve(relativePath));
    }

    @Override
    public ConfigSection createSection(final String relativePath) {
        return document.createSection(resolve(relativePath));
    }

    @Override
    public Object getObject(final String relativePath) {
        return document.get(resolve(relativePath));
    }

    @Override
    public Object getObject(final String relativePath, final Object def) {
        final Object value = getObject(relativePath);
        return value == null ? def : value;
    }

    @Override
    public String getString(final String relativePath) {
        final Object value = getObject(relativePath);
        return value instanceof String ? (String) value : null;
    }

    @Override
    public String getString(final String relativePath, final String def) {
        final String value = getString(relativePath);
        return value == null ? def : value;
    }

    @Override
    public int getInt(final String relativePath) {
        return getInt(relativePath, 0);
    }

    @Override
    public int getInt(final String relativePath, final int def) {
        final Object value = getObject(relativePath);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return def;
    }

    @Override
    public long getLong(final String relativePath) {
        return getLong(relativePath, 0L);
    }

    @Override
    public long getLong(final String relativePath, final long def) {
        final Object value = getObject(relativePath);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return def;
    }

    @Override
    public boolean getBoolean(final String relativePath) {
        return getBoolean(relativePath, false);
    }

    @Override
    public boolean getBoolean(final String relativePath, final boolean def) {
        final Object value = getObject(relativePath);
        if (value instanceof Boolean bool) {
            return bool;
        }
        return def;
    }

    @Override
    public List<?> getArrayList(final String relativePath) {
        final Object value = getObject(relativePath);
        return value instanceof List<?> list ? list : List.of();
    }

    @Override
    public Map<String, Object> getValues(final boolean recursive) {
        if (recursive) {
            final Map<String, Object> values = new LinkedHashMap<>();
            collectValues("", document.sectionMap(path), values);
            return values;
        }
        return new LinkedHashMap<>(document.sectionMap(path));
    }

    private String resolve(final String relativePath) {
        return ConfigPath.join(path, relativePath);
    }

    private void collectKeys(final String prefix, final Map<String, Object> source, final boolean recursive, final Set<String> keys) {
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            final String key = ConfigPath.join(prefix, entry.getKey());
            keys.add(key);
            if (recursive && entry.getValue() instanceof Map<?, ?> map) {
                collectKeys(key, StructuredConfigurationDocument.castMap(map), true, keys);
            }
        }
    }

    private void collectValues(final String prefix, final Map<String, Object> source, final Map<String, Object> target) {
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            final String key = ConfigPath.join(prefix, entry.getKey());
            if (entry.getValue() instanceof Map<?, ?> map) {
                collectValues(key, StructuredConfigurationDocument.castMap(map), target);
            } else {
                target.put(key, entry.getValue());
            }
        }
    }
}
