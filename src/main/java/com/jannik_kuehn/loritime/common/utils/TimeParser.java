package com.jannik_kuehn.loritime.common.utils;

import java.util.HashMap;
import java.util.Map;
import java.util.OptionalLong;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class TimeParser {

    private static final Pattern LONG_PATTERN = Pattern.compile("(\\+|-)?[0-9]+");

    private static final Pattern UNIT_PATTERN = Pattern.compile("\\p{Alpha}+", Pattern.UNICODE_CHARACTER_CLASS);

    private final Map<String, Long> units;

    private final Pattern globalPattern;

    private TimeParser(Map<String, Long> units) {
        this.units = units;
        String unitRegex = units.keySet().stream().collect(Collectors.joining("|", "(", ")"));
        String globalRegex = "(" + LONG_PATTERN + "\\s*" + unitRegex + "\\s*)*(" + LONG_PATTERN + ")?";
        this.globalPattern = Pattern.compile(globalRegex);
    }

    public OptionalLong parseToSeconds(String representation) {
        String raw = representation.trim();
        if (!globalPattern.matcher(raw).matches()) {
            return OptionalLong.empty();
        }
        int position = 0;
        long time = 0L;

        Matcher longMatcher = LONG_PATTERN.matcher(raw);
        Matcher unitMatcher = UNIT_PATTERN.matcher(raw);
        while (position < raw.length()) {
            longMatcher.find(position);
            position = longMatcher.end();
            long unitTime = Long.parseLong(longMatcher.group());
            if (position < raw.length()) {
                unitMatcher.find(position);
                position = unitMatcher.end();
                long unitFactor = units.get(unitMatcher.group());
                time += unitTime * unitFactor;
            } else {
                time += unitTime;
            }
        }
        return OptionalLong.of(time);
    }

    public static class Builder {
        private final Map<String, Long> units = new HashMap<>();

        public Builder addUnit(long inSeconds, String... unitSymbols) {
            if (unitSymbols != null) {
                for (String symbol : unitSymbols) {
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

        public TimeParser build() {
            return new TimeParser(units);
        }
    }
}
