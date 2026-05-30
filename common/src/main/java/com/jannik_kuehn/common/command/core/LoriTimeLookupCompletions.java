package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonSender;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;

/**
 * Live completion source for `/loritime` lookup flags.
 */
public final class LoriTimeLookupCompletions {

    /**
     * Plugin context for live runtime and cached player completions.
     */
    private final LoriTimePlugin plugin;

    /**
     * Creates a completion source.
     *
     * @param plugin plugin context
     */
    public LoriTimeLookupCompletions(final LoriTimePlugin plugin) {
        this.plugin = plugin;
    }

    /**
     * Suggests lookup arguments without database access.
     *
     * @param source command sender
     * @param args current command arguments
     * @return matching completions
     */
    public List<String> suggest(final CommonSender source, final String... args) {
        if (args.length == 0) {
            return suggestArgument(source, "", args);
        }
        final String argument = args[args.length - 1];
        final String lowerArgument = argument.toLowerCase(Locale.ROOT);
        if (lowerArgument.startsWith(CommandScopes.SERVER_PREFIX)) {
            return suggestServerValues(CommandScopes.SERVER_PREFIX, argument);
        }
        if (lowerArgument.startsWith(CommandScopes.SHORT_SERVER_PREFIX)) {
            return suggestServerValues(CommandScopes.SHORT_SERVER_PREFIX, argument);
        }
        if (lowerArgument.startsWith(CommandScopes.WORLD_PREFIX)) {
            return suggestWorldValues(source, CommandScopes.WORLD_PREFIX, argument, args);
        }
        if (lowerArgument.startsWith(CommandScopes.SHORT_WORLD_PREFIX)) {
            return suggestWorldValues(source, CommandScopes.SHORT_WORLD_PREFIX, argument, args);
        }
        if (lowerArgument.startsWith(CommandScopes.TIME_PREFIX) || lowerArgument.startsWith(CommandScopes.SHORT_TIME_PREFIX)) {
            return List.of();
        }
        return suggestArgument(source, argument, args);
    }

    private List<String> suggestArgument(final CommonSender source, final String argument, final String... args) {
        final String[] previousArgs = args.length == 0 ? args : Arrays.copyOf(args, args.length - 1);
        final List<String> suggestions = CommandScopes.suggestScopes(source, argument);
        if (findFlagValue(previousArgs, CommandScopes.SERVER_PREFIX, CommandScopes.SHORT_SERVER_PREFIX).isPresent()) {
            suggestions.remove(CommandScopes.SERVER_PREFIX);
        }
        if (findFlagValue(previousArgs, CommandScopes.WORLD_PREFIX, CommandScopes.SHORT_WORLD_PREFIX).isPresent()) {
            suggestions.remove(CommandScopes.WORLD_PREFIX);
        }
        if (findFlagValue(previousArgs, CommandScopes.TIME_PREFIX, CommandScopes.SHORT_TIME_PREFIX).isPresent()) {
            suggestions.remove(CommandScopes.TIME_PREFIX);
        }
        if (!hasPlayerToken(previousArgs) && source.hasPermission("loritime.see.other")) {
            suggestions.addAll(0, PlayerNameCompletions.suggest(plugin, argument));
        }
        return suggestions;
    }

    private List<String> suggestServerValues(final String prefix, final String argument) {
        return prefixValues(prefix, plugin.getScopeSuggestionCache()
                .suggestServers(plugin.getServer().getLiveServerNames(), valuePrefix(prefix, argument)));
    }

    private List<String> suggestWorldValues(final CommonSender source, final String prefix,
                                            final String argument, final String... args) {
        final Optional<String> serverName = findFlagValue(args, CommandScopes.SERVER_PREFIX, CommandScopes.SHORT_SERVER_PREFIX);
        final Optional<UUID> playerUniqueId = findOnlinePlayerToken(args)
                .or(() -> source instanceof CommonPlayerSender playerSender
                        ? Optional.ofNullable(playerSender.getUniqueId())
                        : Optional.empty());
        return prefixValues(prefix, plugin.getScopeSuggestionCache().suggestWorlds(
                plugin.getServer().getLiveWorldNames(serverName, playerUniqueId), valuePrefix(prefix, argument)));
    }

    private List<String> prefixValues(final String prefix, final List<String> values) {
        return values.stream()
                .map(value -> prefix + value)
                .toList();
    }

    private String valuePrefix(final String prefix, final String argument) {
        return argument.substring(prefix.length());
    }

    private Optional<UUID> findOnlinePlayerToken(final String... args) {
        return Arrays.stream(args)
                .filter(argument -> !argument.contains(":"))
                .findFirst()
                .flatMap(argument -> plugin.getServer().getPlayer(argument))
                .map(CommonPlayerSender::getUniqueId);
    }

    private Optional<String> findFlagValue(final String[] args, final String longPrefix, final String shortPrefix) {
        return Arrays.stream(args)
                .filter(argument -> startsWithIgnoreCase(argument, longPrefix) || startsWithIgnoreCase(argument, shortPrefix))
                .filter(argument -> argument.length() > argument.indexOf(':') + 1)
                .map(argument -> argument.substring(argument.indexOf(':') + 1))
                .findFirst();
    }

    private boolean hasPlayerToken(final String... args) {
        return Arrays.stream(args).anyMatch(argument -> !argument.contains(":"));
    }

    private boolean startsWithIgnoreCase(final String value, final String prefix) {
        return value.regionMatches(true, 0, prefix, 0, prefix.length());
    }
}
