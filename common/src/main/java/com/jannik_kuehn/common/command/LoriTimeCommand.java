package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.common.CommonServer;
import com.jannik_kuehn.common.api.storage.SessionContextDefaults;
import com.jannik_kuehn.common.api.storage.TimeScope;
import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.command.core.CommandScopes;
import com.jannik_kuehn.common.command.core.CommandScopes.LookupRequest;
import com.jannik_kuehn.common.command.core.LoriTimeLookupCompletions;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"PMD.CommentRequired", "PMD.CognitiveComplexity", "PMD.AvoidThrowingRawExceptionTypes"})
public class LoriTimeCommand implements CommonCommand {

    private static final String PLAYER_PLACEHOLDER = "[player]";

    private static final String RANGE_PLACEHOLDER = "[range]";

    private static final String SCOPE_PLACEHOLDER = "[scope]";

    private final LoriTimePlugin loriTimePlugin;

    private final WrappedLogger log;

    private final Localization localization;

    public LoriTimeCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.loriTimePlugin = plugin;
        this.log = plugin.getLoggerFactory().create(LoriTimeCommand.class);
        this.localization = localization;
    }

    @Override
    @SuppressWarnings("PMD.AvoidDeeplyNestedIfStmts")
    public void execute(final CommonSender sender, final String... args) {
        final LookupRequest request = CommandScopes.parseLookup(loriTimePlugin.getParser(), java.time.Clock.systemUTC(), args);
        if (request != null) {
            loriTimePlugin.getScheduler().runAsyncOnce(() -> {
                final LoriTimePlayer targetPlayer;

                if (request.playerName() != null) {
                    final Optional<UUID> optionalPlayer;
                    try {
                        optionalPlayer = loriTimePlugin.getStorage().getUuid(request.playerName());
                    } catch (final StorageException e) {
                        throw new RuntimeException(e);
                    }
                    if (optionalPlayer.isPresent()) {
                        targetPlayer = loriTimePlugin.getPlayerConverter().getOnlinePlayer(optionalPlayer.get());
                    } else {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notFound")
                                .replace(PLAYER_PLACEHOLDER, request.playerName())));
                        return;
                    }
                } else {
                    if (!(sender instanceof CommonPlayerSender playerSender)) {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.consoleSelf")));
                        return;
                    }
                    targetPlayer = loriTimePlugin.getPlayerConverter().getOnlinePlayer(playerSender.getUniqueId());
                }

                final Optional<TimeScope> resolvedScope = resolveScope(request, targetPlayer.getUniqueId());
                if (resolvedScope.isEmpty()) {
                    CommandMessages.send(localization, sender, "message.command.loritime.usage");
                    return;
                }
                final TimeScope scope = resolvedScope.get();
                final boolean isTargetSender = sender instanceof CommonPlayerSender playerSender
                        && targetPlayer.getUniqueId().equals(playerSender.getUniqueId());
                if (!CommandScopes.hasPermission(sender, scope, isTargetSender)) {
                    CommandMessages.send(localization, sender, "message.noPermission");
                    return;
                }

                final long time;
                try {
                    final OptionalLong optionalTime = request.hasTimeRange()
                            ? loriTimePlugin.getStorage().getTime(targetPlayer.getUniqueId(), scope, request.timeRange())
                            : loriTimePlugin.getStorage().getTime(targetPlayer.getUniqueId(), scope);
                    if (optionalTime.isPresent()) {
                        time = optionalTime.getAsLong();
                    } else {
                        final String targetName = loriTimePlugin.getStorage().getName(targetPlayer.getUniqueId())
                                .orElse(targetPlayer.getName());
                        sender.sendMessage(localization.formatTextComponent(noTimeMessage(targetName, scope, request)));
                        return;
                    }
                } catch (final StorageException ex) {
                    log.warn("could not load online time", ex);
                    CommandMessages.send(localization, sender, "message.error");
                    return;
                }
                if (isTargetSender) {
                    sender.sendMessage(localization.formatTextComponent(timeSeenMessage("message.command.loritime.timeSeen.self",
                            targetPlayer.getName(), time, scope, request)));
                } else {
                    try {
                        final String targetName = loriTimePlugin.getStorage().getName(targetPlayer.getUniqueId())
                                .orElse(targetPlayer.getName());
                        sender.sendMessage(localization.formatTextComponent(timeSeenMessage("message.command.loritime.timeSeen.other",
                                targetName, time, scope, request)));
                    } catch (final StorageException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else {
            CommandMessages.send(localization, sender, "message.command.loritime.usage");
        }
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        return new LoriTimeLookupCompletions(loriTimePlugin).suggest(source, args);
    }

    private Optional<TimeScope> resolveScope(final LookupRequest request, final UUID targetUniqueId) {
        if (!request.hasWorld()) {
            return Optional.of(request.hasServer() ? TimeScope.server(request.serverName()) : TimeScope.GLOBAL);
        }
        if (request.hasServer()) {
            return Optional.of(TimeScope.world(request.serverName(), request.worldName()));
        }
        return resolveDefaultServer(targetUniqueId).map(serverName -> TimeScope.world(serverName, request.worldName()));
    }

    private Optional<String> resolveDefaultServer(final UUID targetUniqueId) {
        final CommonServer server = loriTimePlugin.getServer();
        if (server.isProxy()) {
            return server.getCurrentServer(targetUniqueId);
        }
        return server.getLocalServerName().or(() -> Optional.of(SessionContextDefaults.SERVER));
    }

    private String noTimeMessage(final String targetName, final TimeScope scope, final LookupRequest request) {
        if (scope.type() == TimeScope.Type.GLOBAL && !request.hasTimeRange()) {
            return localization.getRawMessage("message.command.loritime.notFound")
                    .replace(PLAYER_PLACEHOLDER, targetName);
        }
        return localization.getRawMessage("message.command.loritime.noScopedTime")
                .replace(PLAYER_PLACEHOLDER, targetName)
                .replace(SCOPE_PLACEHOLDER, scopeDescription(scope, request))
                .replace(RANGE_PLACEHOLDER, rangeDescription(request));
    }

    private String timeSeenMessage(final String messageKey, final String targetName, final long time,
                                   final TimeScope scope, final LookupRequest request) {
        final String scopeText = scopeDescription(scope, request);
        final String rangeText = rangeDescription(request);
        final String rawMessage = localization.getRawMessage(messageKey);
        final String message = rawMessage
                .replace(PLAYER_PLACEHOLDER, targetName)
                .replace("[time]", TimeUtil.formatTime(time, localization))
                .replace(RANGE_PLACEHOLDER, rangeText);
        final StringBuilder result = new StringBuilder(message);
        if (rawMessage.contains(SCOPE_PLACEHOLDER)) {
            final int scopeIndex = result.indexOf(SCOPE_PLACEHOLDER);
            result.replace(scopeIndex, scopeIndex + SCOPE_PLACEHOLDER.length(), scopeText);
        } else {
            result.append(' ').append(scopeText);
        }
        if (!rawMessage.contains(RANGE_PLACEHOLDER)) {
            result.append(rangeText);
        }
        return result.toString();
    }

    private String scopeDescription(final TimeScope scope, final LookupRequest request) {
        return switch (scope.type()) {
            case GLOBAL -> localization.getRawMessage("message.command.loritime.scope.global");
            case SERVER -> localization.getRawMessage("message.command.loritime.scope.server")
                    .replace("[server]", scope.server());
            case WORLD -> worldScopeDescription(scope, request);
        };
    }

    private String worldScopeDescription(final TimeScope scope, final LookupRequest request) {
        final String key = request.hasServer()
                ? "message.command.loritime.scope.worldServer"
                : "message.command.loritime.scope.world";
        return localization.getRawMessage(key)
                .replace("[server]", scope.server())
                .replace("[world]", scope.world());
    }

    private String rangeDescription(final LookupRequest request) {
        if (!request.hasTimeRange()) {
            return "";
        }
        return localization.getRawMessage("message.command.loritime.range")
                .replace(RANGE_PLACEHOLDER, request.timeRangeInput());
    }

    @Override
    public List<String> getAliases() {
        return loriTimePlugin.getConfig().getArrayList("command.LoriTime.alias").stream()
                .filter(item -> item instanceof String)
                .map(item -> (String) item)
                .collect(Collectors.toList());
    }

    @Override
    public String getCommandName() {
        return "loritime";
    }

}
