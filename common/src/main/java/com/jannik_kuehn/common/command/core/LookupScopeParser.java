package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.command.core.CommandScopes.ParsedScopeFlag;
import com.jannik_kuehn.common.command.core.CommandScopes.ScopeFlagType;
import com.jannik_kuehn.common.utils.TimeParser;

import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.OptionalLong;

/**
 * Parses `/loritime` lookup flags.
 */
final class LookupScopeParser {

    /**
     * Number of parts in a single-duration range.
     */
    private static final int SINGLE_RANGE_PARTS = 1;

    /**
     * Number of parts in a near-to-far range.
     */
    private static final int DOUBLE_RANGE_PARTS = 2;

    /**
     * Lower bound for accepted parsed durations.
     */
    private static final long ZERO_SECONDS = 0L;

    private LookupScopeParser() {
    }

    /* default */ static CommandScopes.LookupRequest parse(final TimeParser parser, final Clock clock, final String... args) {
        final LookupState state = new LookupState();
        for (final String argument : args) {
            final ParsedLookupArgument parsedArgument = parseArgument(argument);
            if (parsedArgument == null || !state.accept(parsedArgument)) {
                return null;
            }
        }
        final TimeRange timeRange = state.timeRangeInput == null ? null : parseRange(parser, clock, state.timeRangeInput);
        if (state.timeRangeInput != null && timeRange == null) {
            return null;
        }
        return new CommandScopes.LookupRequest(state.playerName, state.serverName, state.worldName, timeRange, state.timeRangeInput);
    }

    private static ParsedLookupArgument parseArgument(final String argument) {
        final ParsedScopeFlag scopeFlag = CommandScopes.parseScopeFlag(argument);
        if (scopeFlag != null) {
            return new ParsedLookupArgument(scopeFlag.type() == ScopeFlagType.SERVER
                    ? LookupArgumentType.SERVER
                    : LookupArgumentType.WORLD, scopeFlag.value());
        }
        final String lowerArgument = argument.toLowerCase(Locale.ROOT);
        if (lowerArgument.startsWith(CommandScopes.TIME_PREFIX)) {
            return parseFlagArgument(LookupArgumentType.TIME, argument, CommandScopes.TIME_PREFIX.length());
        }
        if (lowerArgument.startsWith(CommandScopes.SHORT_TIME_PREFIX)) {
            return parseFlagArgument(LookupArgumentType.TIME, argument, CommandScopes.SHORT_TIME_PREFIX.length());
        }
        return argument.contains(":") ? null : new ParsedLookupArgument(LookupArgumentType.PLAYER, argument);
    }

    private static ParsedLookupArgument parseFlagArgument(final LookupArgumentType type,
                                                          final String argument, final int prefixLength) {
        if (argument.length() == prefixLength) {
            return null;
        }
        return new ParsedLookupArgument(type, argument.substring(prefixLength));
    }

    private static TimeRange parseRange(final TimeParser parser, final Clock clock, final String value) {
        if (parser == null || clock == null || value.isBlank()) {
            return null;
        }
        final String[] parts = value.split("-", -1);
        return switch (parts.length) {
            case SINGLE_RANGE_PARTS -> parseSingleRange(parser, clock, parts[0]);
            case DOUBLE_RANGE_PARTS -> parseDoubleRange(parser, clock, parts[0], parts[1]);
            default -> null;
        };
    }

    private static TimeRange parseSingleRange(final TimeParser parser, final Clock clock, final String value) {
        final OptionalLong duration = parsePositiveDuration(parser, value);
        return duration.isPresent() ? range(Instant.now(clock), ZERO_SECONDS, duration.getAsLong()) : null;
    }

    private static TimeRange parseDoubleRange(final TimeParser parser, final Clock clock,
                                              final String nearValue, final String farValue) {
        final OptionalLong near = parsePositiveDuration(parser, nearValue);
        final OptionalLong far = parsePositiveDuration(parser, farValue);
        if (near.isEmpty() || far.isEmpty() || near.getAsLong() >= far.getAsLong()) {
            return null;
        }
        return range(Instant.now(clock), near.getAsLong(), far.getAsLong());
    }

    private static OptionalLong parsePositiveDuration(final TimeParser parser, final String value) {
        final OptionalLong duration = parser.parseToSeconds(value);
        if (duration.isEmpty() || duration.getAsLong() <= ZERO_SECONDS) {
            return OptionalLong.empty();
        }
        return duration;
    }

    private static TimeRange range(final Instant now, final long nearSeconds, final long farSeconds) {
        return TimeRange.between(now.minusSeconds(farSeconds), now.minusSeconds(nearSeconds));
    }

    private record ParsedLookupArgument(LookupArgumentType type, String value) {
    }

    /**
     * Mutable parse state for one lookup command.
     */
    private static final class LookupState {
        /**
         * Optional parsed player name.
         */
        private String playerName;

        /**
         * Optional parsed server name.
         */
        private String serverName;

        /**
         * Optional parsed world name.
         */
        private String worldName;

        /**
         * Optional raw time range input.
         */
        private String timeRangeInput;

        private boolean accept(final ParsedLookupArgument argument) {
            return switch (argument.type()) {
                case PLAYER -> assignPlayer(argument.value());
                case SERVER -> assignServer(argument.value());
                case WORLD -> assignWorld(argument.value());
                case TIME -> assignTimeRange(argument.value());
            };
        }

        private boolean assignPlayer(final String value) {
            if (playerName != null) {
                return false;
            }
            playerName = value;
            return true;
        }

        private boolean assignServer(final String value) {
            if (serverName != null) {
                return false;
            }
            serverName = value;
            return true;
        }

        private boolean assignWorld(final String value) {
            if (worldName != null) {
                return false;
            }
            worldName = value;
            return true;
        }

        private boolean assignTimeRange(final String value) {
            if (timeRangeInput != null) {
                return false;
            }
            timeRangeInput = value;
            return true;
        }
    }

    /**
     * Parsed lookup argument type.
     */
    private enum LookupArgumentType {
        /**
         * Non-flag player argument.
         */
        PLAYER,
        /**
         * Server scope flag.
         */
        SERVER,
        /**
         * World scope flag.
         */
        WORLD,
        /**
         * Time range flag.
         */
        TIME
    }
}
