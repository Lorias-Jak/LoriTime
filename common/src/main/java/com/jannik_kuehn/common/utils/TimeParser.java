package com.jannik_kuehn.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Utility class for parsing time representations.
 */
public final class TimeParser {
    /**
     * Pattern for long values.
     */
    private static final Pattern LONG_PATTERN = Pattern.compile("(\\+|-)?[0-9]+");

    /**
     * Pattern for unit values.
     */
    private static final Pattern UNIT_PATTERN = Pattern.compile("\\p{Alpha}+", Pattern.UNICODE_CHARACTER_CLASS);

    /**
     * The custom units.
     */
    private final Map<String, Long> units;

    /**
     * The global pattern.
     */
    private final Pattern globalPattern;

    /**
     * Private constructor to prevent instantiation.
     *
     * @param units the units
     */
    private TimeParser(final Map<String, Long> units) {
        this.units = units;
        final String unitRegex = units.keySet().stream().collect(Collectors.joining("|", "(", ")"));
        final String globalRegex = "(" + LONG_PATTERN + "\\s*" + unitRegex + "\\s*)*(" + LONG_PATTERN + ")?";
        this.globalPattern = Pattern.compile(globalRegex);
    }

    /**
     * Parses the given time representation to seconds.
     *
     * @param representation the time representation
     * @return the time in seconds
     */
    public OptionalLong parseToSeconds(final String representation) {
        final String raw = representation.trim();
        if (!globalPattern.matcher(raw).matches()) {
            return OptionalLong.empty();
        }
        int position = 0;
        long time = 0L;

        final Matcher longMatcher = LONG_PATTERN.matcher(raw);
        final Matcher unitMatcher = UNIT_PATTERN.matcher(raw);
        while (position < raw.length()) {
            longMatcher.find(position);
            position = longMatcher.end();
            final long unitTime = Long.parseLong(longMatcher.group());
            if (position < raw.length()) {
                unitMatcher.find(position);
                position = unitMatcher.end();
                final long unitFactor = units.get(unitMatcher.group());
                time += unitTime * unitFactor;
            } else {
                time += unitTime;
            }
        }
        return OptionalLong.of(time);
    }

    /**
     * Creates a new builder.
     */
    public static class Builder {
        /**
         * The custom units.
         */
        private final Map<String, Long> units = new HashMap<>();

        /**
         * Adds a unit.
         *
         * @param inSeconds   the time in seconds
         * @param unitSymbols the unit symbols
         * @return the builder
         */
        public Builder addUnit(final long inSeconds, final String... unitSymbols) {
            if (unitSymbols != null) {
                for (final String symbol : unitSymbols) {
                    if (!UNIT_PATTERN.matcher(symbol).matches()) {
                        throw new IllegalArgumentException("invalid unit symbol: " + symbol);
                    }
                    if (units.containsKey(symbol)) {
                        throw new IllegalArgumentException("duplicate unit symbol: " + symbol);
                    }
                    units.put(symbol, inSeconds);
                }
            }
            return this;
        }

        /**
         * Builds the time parser.
         *
         * @return the {@link TimeParser}
         */
        public TimeParser build() {
            return new TimeParser(units);
        }
    }
}
