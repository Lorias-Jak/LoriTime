package com.jannik_kuehn.common.config;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Mutable structured configuration document backed by nested maps and lists.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class StructuredConfigurationDocument {
    /**
     * Root map of the document.
     */
    private final Map<String, Object> root;

    /**
     * Creates an empty structured configuration document.
     */
    public StructuredConfigurationDocument() {
        this.root = new LinkedHashMap<>();
    }

    /**
     * Creates a structured configuration document from nested values.
     *
     * @param values nested source values
     */
    public StructuredConfigurationDocument(final Map<?, ?> values) {
        this.root = normalizeMap(values);
    }

    /**
     * Returns the root section.
     *
     * @return root section
     */
    public ConfigSection rootSection() {
        return new MapConfigSection(this, "");
    }

    /**
     * Returns a section at the given path.
     *
     * @param path dot-path section path
     * @return section when present
     */
    public Optional<ConfigSection> getSection(final String path) {
        final Object value = get(path);
        if (value instanceof Map<?, ?>) {
            return Optional.of(new MapConfigSection(this, path));
        }
        return Optional.empty();
    }

    /**
     * Creates or replaces a section at the given path.
     *
     * @param path dot-path section path
     * @return created section
     */
    public ConfigSection createSection(final String path) {
        ensureMap(path);
        return new MapConfigSection(this, path);
    }

    /**
     * Reads a value from a dot path.
     *
     * @param path dot path
     * @return configured value, or null
     */
    public Object get(final String path) {
        if (path == null || path.isBlank()) {
            return root;
        }
        Object current = root;
        for (final String part : ConfigPath.split(path)) {
            if (!(current instanceof Map<?, ?> currentMap)) {
                return null;
            }
            current = currentMap.get(part);
        }
        return current;
    }

    /**
     * Checks whether a dot path exists.
     *
     * @param path dot path
     * @return true when present
     */
    public boolean contains(final String path) {
        return get(path) != null;
    }

    /**
     * Stores a value at a dot path.
     *
     * @param path  dot path
     * @param value value to store
     */
    public void set(final String path, final Object value) {
        final List<String> parts = ConfigPath.split(path);
        if (parts.isEmpty()) {
            if (value instanceof Map<?, ?> map) {
                root.clear();
                root.putAll(normalizeMap(map));
            }
            return;
        }
        final Map<String, Object> parent = ensureParent(parts);
        parent.put(parts.get(parts.size() - 1), normalizeValue(value));
    }

    /**
     * Removes a value at a dot path.
     *
     * @param path dot path
     */
    public void remove(final String path) {
        final List<String> parts = ConfigPath.split(path);
        if (parts.isEmpty()) {
            root.clear();
            return;
        }
        findParent(parts).ifPresent(parent -> parent.remove(parts.get(parts.size() - 1)));
    }

    /**
     * Moves a value from one dot path to another.
     *
     * @param source source path
     * @param target target path
     */
    public void move(final String source, final String target) {
        final Object value = get(source);
        if (value == null) {
            return;
        }
        set(target, value);
        remove(source);
    }

    /**
     * Returns a defensive structured copy.
     *
     * @return nested values
     */
    public Map<String, Object> asMap() {
        return copyMap(root);
    }

    /**
     * Returns flattened scalar/list values.
     *
     * @return flattened dot-path values
     */
    public Map<String, Object> flatten() {
        final Map<String, Object> flattened = new LinkedHashMap<>();
        flattenInto("", root, flattened);
        return flattened;
    }

    /* default */ Map<String, Object> sectionMap(final String path) {
        final Object value = get(path);
        if (value instanceof Map<?, ?> map) {
            return castMap(map);
        }
        return Map.of();
    }

    private void ensureMap(final String path) {
        final List<String> parts = ConfigPath.split(path);
        Map<String, Object> current = root;
        for (final String part : parts) {
            final Object child = current.get(part);
            if (child instanceof Map<?, ?> childMap) {
                current = castMap(childMap);
            } else {
                final Map<String, Object> created = new LinkedHashMap<>();
                current.put(part, created);
                current = created;
            }
        }
    }

    private Map<String, Object> ensureParent(final List<String> parts) {
        Map<String, Object> current = root;
        for (int i = 0; i < parts.size() - 1; i++) {
            final String part = parts.get(i);
            final Object child = current.get(part);
            if (child instanceof Map<?, ?> childMap) {
                current = castMap(childMap);
            } else {
                final Map<String, Object> created = new LinkedHashMap<>();
                current.put(part, created);
                current = created;
            }
        }
        return current;
    }

    private Optional<Map<String, Object>> findParent(final List<String> parts) {
        Map<String, Object> current = root;
        for (int i = 0; i < parts.size() - 1; i++) {
            final Object child = current.get(parts.get(i));
            if (!(child instanceof Map<?, ?> childMap)) {
                return Optional.empty();
            }
            current = castMap(childMap);
        }
        return Optional.of(current);
    }

    private void flattenInto(final String prefix, final Map<String, Object> source, final Map<String, Object> target) {
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            final String path = ConfigPath.join(prefix, entry.getKey());
            if (entry.getValue() instanceof Map<?, ?> map) {
                flattenInto(path, castMap(map), target);
            } else {
                target.put(path, entry.getValue());
            }
        }
    }

    /* default */ static Object normalizeValue(final Object value) {
        if (value instanceof Map<?, ?> map) {
            return normalizeMap(map);
        }
        if (value instanceof List<?> list) {
            final List<Object> normalized = new ArrayList<>();
            for (final Object item : list) {
                normalized.add(normalizeValue(item));
            }
            return normalized;
        }
        return value;
    }

    /* default */ static Map<String, Object> normalizeMap(final Map<?, ?> values) {
        final Map<String, Object> normalized = new LinkedHashMap<>();
        if (values == null) {
            return normalized;
        }
        for (final Map.Entry<?, ?> entry : values.entrySet()) {
            if (entry.getKey() != null) {
                normalized.put(entry.getKey().toString(), normalizeValue(entry.getValue()));
            }
        }
        return normalized;
    }

    /* default */ static Map<String, Object> copyMap(final Map<String, Object> source) {
        final Map<String, Object> copy = new LinkedHashMap<>();
        for (final Map.Entry<String, Object> entry : source.entrySet()) {
            copy.put(entry.getKey(), normalizeValue(entry.getValue()));
        }
        return copy;
    }

    @SuppressWarnings("unchecked")
    /* default */ static Map<String, Object> castMap(final Map<?, ?> map) {
        return (Map<String, Object>) map;
    }
}
