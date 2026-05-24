package com.jannik_kuehn.common.command.core;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared command completion helpers.
 */
public final class CommandCompletions {

    private CommandCompletions() {
    }

    /**
     * Filters values by prefix.
     *
     * @param values completion values
     * @param prefix current prefix
     * @return filtered values
     */
    public static List<String> startsWith(final List<String> values, final String prefix) {
        final String normalizedPrefix = prefix == null ? "" : prefix.toLowerCase(Locale.ROOT);
        final List<String> results = new ArrayList<>();
        for (final String value : values) {
            if (value.toLowerCase(Locale.ROOT).startsWith(normalizedPrefix)) {
                results.add(value);
            }
        }
        return results;
    }
}
