package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.common.utils.TimeParser;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared parser and permission helpers for scoped time commands.
 */
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
     * Command token for server scope suffixes.
     */
    public static final String SERVER = "server";

    /**
     * Command token for world scope suffixes.
     */
    public static final String WORLD = "world";

    private static final int MIN_TIMED_SCOPE_ARGS = 2;

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
     * Suggests scope argument prefixes allowed for the sender.
     *
     * @param source   command sender
     * @param argument partially typed argument
     * @return scope suggestions
     */
    public static List<String> suggestScopes(final CommonSender source, final String argument) {
        final String lowerArgument = argument.toLowerCase(Locale.ROOT);
        final List<String> suggestions = new ArrayList<>();
        if (source.hasPermission("loritime.see.server") && SERVER_PREFIX.startsWith(lowerArgument)) {
            suggestions.add(SERVER_PREFIX);
        }
        if (source.hasPermission("loritime.see.world") && WORLD_PREFIX.startsWith(lowerArgument)) {
            suggestions.add(WORLD_PREFIX);
        }
        if (TIME_PREFIX.startsWith(lowerArgument)) {
            suggestions.add(TIME_PREFIX);
        }
        return suggestions;
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
     * Parses a duration plus optional scope suffix.
     *
     * @param args command arguments
     * @return parsed timed scope, or null for invalid input
     */
    public static ParsedTimedScope parseTimedScope(final String... args) {
        if (args.length < MIN_TIMED_SCOPE_ARGS) {
            return null;
        }
        final int scopeStart = findScopeStart(args, 1);
        final String[] timeArgs = java.util.Arrays.copyOfRange(args, 1, scopeStart < 0 ? args.length : scopeStart);
        final TimeScope scope = scopeStart < 0 ? TimeScope.GLOBAL : parseScopeSuffix(args, scopeStart);
        return scope == null || timeArgs.length == 0 ? null : new ParsedTimedScope(timeArgs, scope);
    }

    /**
     * Parses a target player plus optional reset scope.
     *
     * @param args command arguments
     * @return parsed reset scope, or null for invalid input
     */
    public static ParsedScope parseResetScope(final String... args) {
        if (args.length == RESET_WITHOUT_SCOPE_ARGS) {
            return new ParsedScope(args[0], TimeScope.GLOBAL);
        }
        final TimeScope scope = parseScopeSuffix(args, 1);
        return scope == null ? null : new ParsedScope(args[0], scope);
    }

    private static int findScopeStart(final String[] args, final int start) {
        for (int index = start; index < args.length; index++) {
            if (SERVER.equalsIgnoreCase(args[index]) || WORLD.equalsIgnoreCase(args[index])) {
                return index;
            }
        }
        return -1;
    }

    private static TimeScope parseScopeSuffix(final String[] args, final int start) {
        if (SERVER.equalsIgnoreCase(args[start]) && args.length == start + 2) {
            return TimeScope.server(args[start + 1]);
        }
        if (WORLD.equalsIgnoreCase(args[start]) && args.length == start + 3) {
            return TimeScope.world(args[start + 1], args[start + 2]);
        }
        return null;
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
     * @param scope    target scope
     */
    public record ParsedTimedScope(String[] timeArgs, TimeScope scope) {
    }

    /**
     * Parsed player target with scope.
     *
     * @param playerName target player name
     * @param scope      target scope
     */
    public record ParsedScope(String playerName, TimeScope scope) {
    }
}
