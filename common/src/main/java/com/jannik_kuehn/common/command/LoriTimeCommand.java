package com.jannik_kuehn.common.command;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.command.core.PlayerNameCompletions;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"PMD.CommentRequired", "PMD.AvoidLiteralsInIfCondition", "PMD.CognitiveComplexity",
        "PMD.AvoidThrowingRawExceptionTypes"})
public class LoriTimeCommand implements CommonCommand {

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
        if (!sender.hasPermission("loritime.see")) {
            CommandMessages.send(localization, sender, "message.nopermission");
            return;
        }
        if (args.length <= 1) {
            loriTimePlugin.getScheduler().runAsyncOnce(() -> {
                final LoriTimePlayer targetPlayer;

                if (args.length == 1) {
                    final Optional<UUID> optionalPlayer;
                    try {
                        optionalPlayer = loriTimePlugin.getStorage().getUuid(args[0]);
                    } catch (final StorageException e) {
                        throw new RuntimeException(e);
                    }
                    if (optionalPlayer.isPresent()) {
                        targetPlayer = loriTimePlugin.getPlayerConverter().getOnlinePlayer(optionalPlayer.get());
                    } else {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notfound")
                                .replace("[player]", args[0])));
                        return;
                    }
                } else {
                    if (!(sender instanceof CommonPlayerSender playerSender)) {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.consoleself")));
                        return;
                    }
                    targetPlayer = loriTimePlugin.getPlayerConverter().getOnlinePlayer(playerSender.getUniqueId());
                }

                final boolean isTargetSender = sender instanceof CommonPlayerSender playerSender
                        && targetPlayer.getUniqueId().equals(playerSender.getUniqueId());
                    if (!isTargetSender && !sender.hasPermission("loritime.see.other")) {
                    CommandMessages.send(localization, sender, "message.nopermission");
                    return;
                }

                final long time;
                try {
                    final OptionalLong optionalTime = loriTimePlugin.getStorage().getTime(targetPlayer.getUniqueId());
                    if (optionalTime.isPresent()) {
                        time = optionalTime.getAsLong();
                    } else {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notfound")
                                .replace("[player]", loriTimePlugin.getStorage().getName(targetPlayer.getUniqueId()).get())));
                        return;
                    }
                } catch (final StorageException ex) {
                    log.warn("could not load online time", ex);
                    CommandMessages.send(localization, sender, "message.error");
                    return;
                }
                if (isTargetSender) {
                    sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.timeseen.self")
                            .replace("[time]", TimeUtil.formatTime(time, localization))));
                } else {
                    try {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.timeseen.other")
                                .replace("[player]", loriTimePlugin.getStorage().getName(targetPlayer.getUniqueId()).get())
                                .replace("[time]", TimeUtil.formatTime(time, localization))));
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
        if (!source.hasPermission("loritime.see.other")) {
            return new ArrayList<>();
        }

        if (args.length == 0) {
            return PlayerNameCompletions.suggest(loriTimePlugin, "");
        }
        if (args.length == 1) {
            return PlayerNameCompletions.suggest(loriTimePlugin, args[0]);
        }
        return new ArrayList<>();
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
