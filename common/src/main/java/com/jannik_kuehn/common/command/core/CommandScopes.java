package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.utils.TimeParser;

import java.time.Clock;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Shared parser and permission helpers for scoped time commands.
 */
@SuppressWarnings("PMD.CommentRequired")
public final class CommandScopes {
    private static final int MIN_TIMED_SCOPE_ARGS = 2;

    private static final int RESET_WITHOUT_SCOPE_ARGS = 1;

    public static final String SERVER_PREFIX = "server:";

    public static final String WORLD_PREFIX = "world:";

    public static final String SHORT_SERVER_PREFIX = "s:";

    public static final String SHORT_WORLD_PREFIX = "w:";

    public static final String TIME_PREFIX = "time:";

    public static final String SHORT_TIME_PREFIX = "t:";

    public static final String SERVER = "server";

    public static final String WORLD = "world";

    private CommandScopes() {
    }

    public static LookupRequest parseLookup(final String... args) {
        return LookupScopeParser.parse(null, Clock.systemUTC(), args);
    }

    public static LookupRequest parseLookup(final TimeParser parser, final Clock clock, final String... args) {
        return LookupScopeParser.parse(parser, clock, args);
    }

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

    public static boolean hasPermission(final CommonSender sender, final TimeScope scope, final boolean self) {
        return switch (scope.type()) {
            case GLOBAL -> sender.hasPermission(self ? "loritime.see" : "loritime.see.other");
            case SERVER -> sender.hasPermission(self ? "loritime.see.server" : "loritime.see.server.other");
            case WORLD -> sender.hasPermission(self ? "loritime.see.world" : "loritime.see.world.other");
        };
    }

    public static ParsedTimedScope parseTimedScope(final String... args) {
        if (args.length < MIN_TIMED_SCOPE_ARGS) {
            return null;
        }
        final int scopeStart = findScopeStart(args, 1);
        final String[] timeArgs = java.util.Arrays.copyOfRange(args, 1, scopeStart < 0 ? args.length : scopeStart);
        final TimeScope scope = scopeStart < 0 ? TimeScope.GLOBAL : parseScopeSuffix(args, scopeStart);
        return scope == null || timeArgs.length == 0 ? null : new ParsedTimedScope(timeArgs, scope);
    }

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

    public record LookupRequest(String playerName, String serverName, String worldName,
                                TimeRange timeRange, String timeRangeInput) {
        public LookupRequest(final String playerName, final String serverName, final String worldName) {
            this(playerName, serverName, worldName, null, null);
        }

        public boolean hasServer() {
            return serverName != null;
        }

        public boolean hasWorld() {
            return worldName != null;
        }

        public boolean hasTimeRange() {
            return timeRange != null;
        }
    }

    public record ParsedTimedScope(String[] timeArgs, TimeScope scope) {
    }

    public record ParsedScope(String playerName, TimeScope scope) {
    }
}
