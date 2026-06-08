package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.common.utils.TimeParser;

import java.time.Clock;
import java.util.Arrays;
import java.util.Locale;

/**
 * Shared parser and permission helpers for scoped time commands.
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class CommandScopes {
    /**
     * Prefix for explicit server-scope lookup arguments.
     */
    public static final String SERVER_PREFIX = "server:";

    /**
     * Prefix for explicit world-scope lookup arguments.
     */
    public static final String WORLD_PREFIX = "world:";

    /**
     * Short prefix for explicit server-scope lookup arguments.
     */
    public static final String SHORT_SERVER_PREFIX = "s:";

    /**
     * Short prefix for explicit world-scope lookup arguments.
     */
    public static final String SHORT_WORLD_PREFIX = "w:";

    /**
     * Prefix for explicit ranged-time lookup arguments.
     */
    public static final String TIME_PREFIX = "time:";

    /**
     * Short prefix for explicit ranged-time lookup arguments.
     */
    public static final String SHORT_TIME_PREFIX = "t:";

    /**
     * Command token for legacy server scope suffixes.
     */
    public static final String SERVER = "server";

    /**
     * Command token for legacy world scope suffixes.
     */
    public static final String WORLD = "world";

    /**
     * Minimum number of arguments for timed modify operations.
     */
    private static final int MIN_TIMED_SCOPE_ARGS = 2;

    /**
     * Number of arguments before reset scope flags.
     */
    private static final int RESET_WITHOUT_SCOPE_ARGS = 1;

    private CommandScopes() {
    }

    /**
     * Parses lookup arguments using the system UTC clock.
     *
     * @param args command arguments
     * @return parsed lookup request
     */
    public static LookupRequest parseLookup(final String... args) {
        return LookupScopeParser.parse(null, Clock.systemUTC(), args);
    }

    /**
     * Parses lookup arguments with explicit time parsing dependencies.
     *
     * @param parser time parser for ranged lookups
     * @param clock  clock used for relative ranges
     * @param args   command arguments
     * @return parsed lookup request
     */
    public static LookupRequest parseLookup(final TimeParser parser, final Clock clock, final String... args) {
        return LookupScopeParser.parse(parser, clock, args);
    }

    /**
     * Parses one server or world scope flag argument.
     *
     * @param argument command argument
     * @return parsed argument, or null when the argument is not a valid scope flag
     */
    /* default */ static ParsedScopeFlag parseScopeFlag(final String argument) {
        final String lowerArgument = argument.toLowerCase(Locale.ROOT);
        if (lowerArgument.startsWith(SERVER_PREFIX)) {
            return parseFlagArgument(ScopeFlagType.SERVER, argument, SERVER_PREFIX.length());
        }
        if (lowerArgument.startsWith(SHORT_SERVER_PREFIX)) {
            return parseFlagArgument(ScopeFlagType.SERVER, argument, SHORT_SERVER_PREFIX.length());
        }
        if (lowerArgument.startsWith(WORLD_PREFIX)) {
            return parseFlagArgument(ScopeFlagType.WORLD, argument, WORLD_PREFIX.length());
        }
        if (lowerArgument.startsWith(SHORT_WORLD_PREFIX)) {
            return parseFlagArgument(ScopeFlagType.WORLD, argument, SHORT_WORLD_PREFIX.length());
        }
        return null;
    }

    private static ParsedScopeFlag parseFlagArgument(final ScopeFlagType type,
                                                     final String argument, final int prefixLength) {
        if (argument.length() == prefixLength) {
            return null;
        }
        return new ParsedScopeFlag(type, argument.substring(prefixLength));
    }

    /**
     * Parses only server/world scope flags.
     *
     * @param args scope flag arguments
     * @return parsed scope flags, or null for invalid or duplicate flags
     */
    public static ParsedScopeFlags parseScopeFlags(final String... args) {
        final ScopeFlagState state = new ScopeFlagState();
        for (final String argument : args) {
            final ParsedScopeFlag parsedFlag = parseScopeFlag(argument);
            if (parsedFlag == null || !state.accept(parsedFlag)) {
                return null;
            }
        }
        return new ParsedScopeFlags(state.serverName, state.worldName);
    }

    /**
     * Checks whether a sender may read a scope.
     *
     * @param sender command sender
     * @param scope  requested time scope
     * @param self   true when the sender queries their own time
     * @return true when the sender has permission
     */
    public static boolean hasPermission(final CommonSender sender, final TimeScope scope, final boolean self) {
        return switch (scope.type()) {
            case GLOBAL -> sender.hasPermission(self ? "loritime.see" : "loritime.see.other");
            case SERVER -> sender.hasPermission(self ? "loritime.see.server" : "loritime.see.server.other");
            case WORLD -> sender.hasPermission(self ? "loritime.see.world" : "loritime.see.world.other");
        };
    }

    /**
     * Checks whether a sender may read a bounded time range.
     *
     * @param sender command sender
     * @param self   true when the sender queries their own time
     * @return true when the sender has permission
     */
    public static boolean hasTimeRangePermission(final CommonSender sender, final boolean self) {
        return sender.hasPermission(self ? "loritime.see.timerange" : "loritime.see.timerange.other");
    }

    /**
     * Parses a duration plus optional scope suffix.
     *
     * @param args command arguments
     * @return parsed timed scope, or null for invalid input
     */
    public static ParsedTimedScope parseTimedScope(final String... args) {
        if (args.length < MIN_TIMED_SCOPE_ARGS) {
            return null;
        }
        final int scopeStart = findFlagScopeStart(args, 1);
        final String[] timeArgs = Arrays.copyOfRange(args, 1, scopeStart < 0 ? args.length : scopeStart);
        if (timeArgs.length == 0 || containsInvalidModifyToken(timeArgs)) {
            return null;
        }
        final ParsedScopeFlags scopeFlags = scopeStart < 0
                ? ParsedScopeFlags.GLOBAL
                : parseScopeFlags(Arrays.copyOfRange(args, scopeStart, args.length));
        return scopeFlags == null ? null : new ParsedTimedScope(timeArgs, scopeFlags);
    }

    /**
     * Parses a target player plus optional reset scope.
     *
     * @param args command arguments
     * @return parsed reset scope, or null for invalid input
     */
    public static ParsedScope parseResetScope(final String... args) {
        if (args.length == RESET_WITHOUT_SCOPE_ARGS) {
            return new ParsedScope(args[0], ParsedScopeFlags.GLOBAL);
        }
        final String[] scopeArgs = Arrays.copyOfRange(args, 1, args.length);
        final ParsedScopeFlags scopeFlags = parseScopeFlags(scopeArgs);
        return scopeFlags == null ? null : new ParsedScope(args[0], scopeFlags);
    }

    private static int findFlagScopeStart(final String[] args, final int start) {
        for (int index = start; index < args.length; index++) {
            if (parseScopeFlag(args[index]) != null) {
                return index;
            }
        }
        return -1;
    }

    private static boolean containsInvalidModifyToken(final String... timeArgs) {
        return Arrays.stream(timeArgs)
                .anyMatch(argument -> argument.contains(":")
                        || SERVER.equalsIgnoreCase(argument)
                        || WORLD.equalsIgnoreCase(argument));
    }

    /**
     * Parsed lookup command arguments.
     *
     * @param playerName     optional player name
     * @param serverName     optional server name
     * @param worldName      optional world name
     * @param timeRange      optional bounded time range
     * @param timeRangeInput original time range input
     */
    public record LookupRequest(String playerName, String serverName, String worldName,
                                TimeRange timeRange, String timeRangeInput) {
        /**
         * Creates a lookup request without a time range.
         *
         * @param playerName optional player name
         * @param serverName optional server name
         * @param worldName  optional world name
         */
        public LookupRequest(final String playerName, final String serverName, final String worldName) {
            this(playerName, serverName, worldName, null, null);
        }

        /**
         * Returns whether a server scope was supplied.
         *
         * @return true when a server name is present
         */
        public boolean hasServer() {
            return serverName != null;
        }

        /**
         * Returns whether a world scope was supplied.
         *
         * @return true when a world name is present
         */
        public boolean hasWorld() {
            return worldName != null;
        }

        /**
         * Returns whether a time range was supplied.
         *
         * @return true when a time range is present
         */
        public boolean hasTimeRange() {
            return timeRange != null;
        }
    }

    /**
     * Parsed time amount arguments with their target scope.
     *
     * @param timeArgs time amount arguments
     * @param scopeFlags target scope flags
     */
    public record ParsedTimedScope(String[] timeArgs, ParsedScopeFlags scopeFlags) {
    }

    /**
     * Parsed player target with scope.
     *
     * @param playerName target player name
     * @param scopeFlags target scope flags
     */
    public record ParsedScope(String playerName, ParsedScopeFlags scopeFlags) {
    }

    /**
     * Parsed server/world scope flags.
     *
     * @param serverName optional server name
     * @param worldName  optional world name
     */
    public record ParsedScopeFlags(String serverName, String worldName) {
        /**
         * Empty scope flag set for global operations.
         */
        public static final ParsedScopeFlags GLOBAL = new ParsedScopeFlags(null, null);

        /**
         * Returns whether a server flag was supplied.
         *
         * @return true when a server name is present
         */
        public boolean hasServer() {
            return serverName != null;
        }

        /**
         * Returns whether a world flag was supplied.
         *
         * @return true when a world name is present
         */
        public boolean hasWorld() {
            return worldName != null;
        }
    }

    /* default */ record ParsedScopeFlag(ScopeFlagType type, String value) {
    }

    /**
     * Supported server/world scope flag types.
     */
    /* default */ enum ScopeFlagType {
        /**
         * Server scope flag.
         */
        SERVER,
        /**
         * World scope flag.
         */
        WORLD
    }

    /**
     * Mutable parse state for server/world scope flags.
     */
    private static final class ScopeFlagState {
        /**
         * Optional parsed server name.
         */
        private String serverName;

        /**
         * Optional parsed world name.
         */
        private String worldName;

        private boolean accept(final ParsedScopeFlag flag) {
            return switch (flag.type()) {
                case SERVER -> assignServer(flag.value());
                case WORLD -> assignWorld(flag.value());
            };
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
    }
}
