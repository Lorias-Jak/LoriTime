package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.platform.CommonSender;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/**
 * Suggests scope and range flag prefixes for lookup commands.
 */
final class LookupScopeSuggestions {

    private LookupScopeSuggestions() {
    }

    /**
     * Suggests lookup flag prefixes.
     *
     * @param source                  command sender
     * @param argument                partially typed argument
     * @param includeTimeRange        true when time range flags should be suggested
     * @param requireScopePermissions true when read permissions should gate suggestions
     * @return matching flag prefix suggestions
     */
    /* default */ static List<String> suggest(final CommonSender source, final String argument,
                                              final boolean includeTimeRange,
                                              final boolean requireScopePermissions) {
        final String lowerArgument = argument.toLowerCase(Locale.ROOT);
        final List<String> suggestions = new ArrayList<>();
        suggestServer(source, lowerArgument, requireScopePermissions, suggestions);
        suggestWorld(source, lowerArgument, requireScopePermissions, suggestions);
        suggestTimeRange(source, lowerArgument, includeTimeRange, requireScopePermissions, suggestions);
        return suggestions;
    }

    private static void suggestServer(final CommonSender source, final String lowerArgument,
                                      final boolean requireScopePermissions, final List<String> suggestions) {
        if ((!requireScopePermissions || source.hasPermission("loritime.see.server"))
                && CommandScopes.SERVER_PREFIX.startsWith(lowerArgument)) {
            suggestions.add(CommandScopes.SERVER_PREFIX);
        }
    }

    private static void suggestWorld(final CommonSender source, final String lowerArgument,
                                     final boolean requireScopePermissions, final List<String> suggestions) {
        if ((!requireScopePermissions || source.hasPermission("loritime.see.world"))
                && CommandScopes.WORLD_PREFIX.startsWith(lowerArgument)) {
            suggestions.add(CommandScopes.WORLD_PREFIX);
        }
    }

    private static void suggestTimeRange(final CommonSender source, final String lowerArgument,
                                         final boolean includeTimeRange,
                                         final boolean requireScopePermissions,
                                         final List<String> suggestions) {
        if (includeTimeRange && canSuggestTimeRange(source, requireScopePermissions)
                && CommandScopes.TIME_PREFIX.startsWith(lowerArgument)) {
            suggestions.add(CommandScopes.TIME_PREFIX);
        }
    }

    private static boolean canSuggestTimeRange(final CommonSender source, final boolean requireScopePermissions) {
        return !requireScopePermissions
                || source.hasPermission("loritime.see.timerange")
                || source.hasPermission("loritime.see.timerange.other");
    }
}
