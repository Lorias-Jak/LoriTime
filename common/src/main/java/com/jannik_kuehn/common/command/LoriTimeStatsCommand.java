package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.storage.TimeRange;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.command.core.CommandCompletions;
import com.jannik_kuehn.common.command.core.CommandScopes;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.platform.CommonCommand;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.common.storage.model.SessionContextDefaults;
import com.jannik_kuehn.common.storage.model.StatisticsRequest;
import com.jannik_kuehn.common.storage.model.StatisticsSnapshot;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.OptionalLong;

/** Canonical bounded network statistics command. */
@SuppressWarnings({"PMD.CommentRequired", "PMD.LiteralsFirstInComparisons", "PMD.AvoidLiteralsInIfCondition",
        "PMD.TooManyMethods", "PMD.CyclomaticComplexity"})
public class LoriTimeStatsCommand implements CommonCommand {
    private static final long DEFAULT_BOUNCE_SECONDS = 180L;

    private static final String SERVER_SELECTOR = "server:";

    private static final String WORLD_SELECTOR = "world:";

    private static final List<String> VIEWS = List.of("users", "sessions", "usage", "top", "afk", "retention");

    private final LoriTimePlugin plugin;

    private final Localization localization;

    private final WrappedLogger log;

    private final Clock clock;

    public LoriTimeStatsCommand(final LoriTimePlugin plugin, final Localization localization) {
        this(plugin, localization, Clock.systemUTC());
    }

    /* default */ LoriTimeStatsCommand(final LoriTimePlugin plugin, final Localization localization, final Clock clock) {
        this.plugin = plugin;
        this.localization = localization;
        this.log = plugin.getLoggerFactory().create(LoriTimeStatsCommand.class);
        this.clock = clock;
    }

    @Override
    public void execute(final CommonSender sender, final String... arguments) {
        final String view = arguments.length > 0 && VIEWS.contains(arguments[0].toLowerCase(Locale.ROOT))
                ? arguments[0].toLowerCase(Locale.ROOT) : "overview";
        final String[] flags = view.equals("overview") ? arguments : Arrays.copyOfRange(arguments, 1, arguments.length);
        final long bounce = positiveDuration("stats.bounce-threshold", "3m", DEFAULT_BOUNCE_SECONDS);
        final String defaultRange = plugin.getConfig().getString("stats.default-range", "calendar:today");
        final ZoneId calendarZone = StatisticsRangeParser.resolveZone(
                plugin.getConfig().getString("stats.calendar-time-zone", "system"), ZoneId.systemDefault(), log::warn);
        final StatisticsRangeParser.Selection selection = StatisticsRangeParser.parse(plugin.getParser(), clock,
                calendarZone, defaultRange, flags);
        if (selection == null) {
            send(sender, "commandUsage", Map.of());
            return;
        }
        final CommandScopes.LookupRequest parsed = selection.lookup();
        final TimeRange range = selection.range();
        final String server = parsed.serverName() == null && parsed.worldName() != null
                ? plugin.getConfig().getString("server.name", SessionContextDefaults.SERVER) : parsed.serverName();
        final TimeScope scope = parsed.worldName() != null ? TimeScope.world(server, parsed.worldName())
                : parsed.serverName() != null ? TimeScope.server(parsed.serverName()) : TimeScope.GLOBAL;
        plugin.getStatisticsStorage().ifPresentOrElse(storage -> {
            try {
                render(sender, view, storage.getStatistics(new StatisticsRequest(range, scope,
                                Duration.ofSeconds(bounce), selection.observedAt())), selection.label(), scope);
            } catch (final StorageException ex) {
                log.error("Could not load LoriTime statistics.", ex);
                send(sender, "error", Map.of());
            }
        }, () -> send(sender, "unsupported", Map.of()));
    }

    private long positiveDuration(final String key, final String fallback, final long secondsFallback) {
        final OptionalLong parsed = plugin.getParser().parseToSeconds(plugin.getConfig().getString(key, fallback));
        return parsed.isPresent() && parsed.getAsLong() > 0L ? parsed.getAsLong() : secondsFallback;
    }

    private void render(final CommonSender sender, final String view, final StatisticsSnapshot snapshot,
                        final String range, final TimeScope scope) {
        final long totalUsageSeconds = snapshot.serverUsage().values().stream().mapToLong(Duration::getSeconds).sum();
        final String usage = snapshot.serverUsage().entrySet().stream().limit(10)
                .map(entry -> entry.getKey() + ": " + format(entry.getValue()) + " ("
                        + percent(totalUsageSeconds == 0L ? 0.0D
                        : (double) entry.getValue().getSeconds() / totalUsageSeconds) + ")")
                .reduce((left, right) -> left + "<newline>" + right).orElse("-");
        final String top = snapshot.topPlayers().stream().limit(10)
                .map(entry -> entry.name() + ": " + format(entry.duration()))
                .reduce((left, right) -> left + "<newline>" + right).orElse("-");
        send(sender, view, Map.ofEntries(
                Map.entry("range", range), Map.entry("scope", scopeLabel(scope)),
                Map.entry("unique", String.valueOf(snapshot.uniqueUsers())),
                Map.entry("new", String.valueOf(snapshot.newUsers())),
                Map.entry("sessions", String.valueOf(snapshot.sessions())),
                Map.entry("playtime", format(snapshot.totalPlayTime())),
                Map.entry("median", format(snapshot.medianSession())),
                Map.entry("longest", format(snapshot.longestSession())),
                Map.entry("peruser", String.format(Locale.ROOT, "%.2f", snapshot.sessionsPerUser())),
                Map.entry("bounces", String.valueOf(snapshot.bounces())),
                Map.entry("bounce", percent(snapshot.bounceRate())),
                Map.entry("peak", String.valueOf(snapshot.peakConcurrent())),
                Map.entry("afkplayers", String.valueOf(snapshot.afkPlayers())),
                Map.entry("afkkicks", String.valueOf(snapshot.afkKicks())),
                Map.entry("afktime", format(snapshot.afkDuration())),
                Map.entry("afkperiods", String.valueOf(snapshot.afkPeriods())),
                Map.entry("retention", percent(snapshot.retentionRate())),
                Map.entry("usage", usage), Map.entry("top", top)));
    }

    private String scopeLabel(final TimeScope scope) {
        return switch (scope.type()) {
            case GLOBAL -> "network";
            case SERVER -> scope.server();
            case WORLD -> scope.server() + "/" + scope.world();
        };
    }

    private String format(final Duration duration) {
        return TimeUtil.formatTime(duration.getSeconds(), localization);
    }

    private String percent(final double ratio) {
        return String.format(Locale.ROOT, "%.1f%%", ratio * 100.0D);
    }

    private void send(final CommonSender sender, final String key, final Map<String, String> replacements) {
        String message = localization.getRawMessage("message.command.stats." + key);
        for (final Map.Entry<String, String> replacement : replacements.entrySet()) {
            message = message.replace("[" + replacement.getKey() + "]", replacement.getValue());
        }
        sender.sendMessage(localization.formatTextComponentWithoutPrefix(message));
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        final String current = args.length == 0 ? "" : args[args.length - 1].toLowerCase(Locale.ROOT);
        final List<String> previous = Arrays.stream(args, 0, Math.max(0, args.length - 1))
                .map(value -> value.toLowerCase(Locale.ROOT)).toList();
        if (current.startsWith(SERVER_SELECTOR)) {
            if (containsSelector(previous, SERVER_SELECTOR)) {
                return List.of();
            }
            return prefixed(SERVER_SELECTOR, plugin.getScopeSuggestionCache().suggestServers(List.of(),
                    current.substring(SERVER_SELECTOR.length())));
        }
        if (current.startsWith(WORLD_SELECTOR)) {
            if (containsSelector(previous, WORLD_SELECTOR)) {
                return List.of();
            }
            final String server = valueFor(previous, SERVER_SELECTOR);
            final List<String> worlds = server == null
                    ? plugin.getScopeSuggestionCache().suggestWorlds(List.of(), current.substring(WORLD_SELECTOR.length()))
                    : plugin.getScopeSuggestionCache().suggestWorlds(server, List.of(), current.substring(WORLD_SELECTOR.length()));
            return prefixed(WORLD_SELECTOR, worlds);
        }
        final List<String> suggestions = new ArrayList<>();
        if (args.length <= 1) {
            suggestions.addAll(VIEWS);
        }
        if (!containsSelector(previous, "time:") && !containsSelector(previous, "calendar:")) {
            suggestions.addAll(List.of("time:1d", "calendar:today", "calendar:this-week", "calendar:this-month",
                    "calendar:2d", "calendar:2d-3d", "calendar:2w", "calendar:2w-3w", "calendar:2mo",
                    "calendar:2mo-3mo"));
        }
        if (!containsSelector(previous, SERVER_SELECTOR)) {
            suggestions.add(SERVER_SELECTOR);
        }
        if (!containsSelector(previous, WORLD_SELECTOR)) {
            suggestions.add(WORLD_SELECTOR);
        }
        return CommandCompletions.startsWith(suggestions, current);
    }

    private List<String> prefixed(final String prefix, final List<String> values) {
        return values.stream().map(value -> prefix + value).toList();
    }

    private boolean containsSelector(final List<String> values, final String selector) {
        return values.stream().anyMatch(value -> value.startsWith(selector));
    }

    private String valueFor(final List<String> values, final String selector) {
        return values.stream().filter(value -> value.startsWith(selector))
                .map(value -> value.substring(selector.length())).findFirst().orElse(null);
    }

    @Override
    public List<String> getAliases() {
        return List.of("stats", "loritimestats");
    }

    @Override
    public String getCommandName() {
        return "ltstats";
    }
}
