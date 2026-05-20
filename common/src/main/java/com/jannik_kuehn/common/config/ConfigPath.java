package com.jannik_kuehn.common.config;

import java.util.ArrayList;
import java.util.List;

/**
 * Utility methods for dot-separated configuration paths.
 */
final class ConfigPath {
    private ConfigPath() {
    }

    /* default */ static List<String> split(final String path) {
        final List<String> parts = new ArrayList<>();
        if (path == null || path.isBlank()) {
            return parts;
        }
        for (final String part : path.split("\\.")) {
            if (!part.isBlank()) {
                parts.add(part);
            }
        }
        return parts;
    }

    /* default */ static String join(final String first, final String second) {
        if (first == null || first.isBlank()) {
            return second == null ? "" : second;
        }
        if (second == null || second.isBlank()) {
            return first;
        }
        return first + "." + second;
    }
}
