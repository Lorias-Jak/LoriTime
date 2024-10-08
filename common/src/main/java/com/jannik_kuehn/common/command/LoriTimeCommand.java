package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;

@SuppressWarnings({"PMD.CommentRequired", "PMD.TooManyMethods", "PMD.AvoidLiteralsInIfCondition", "PMD.CognitiveComplexity",
        "PMD.CyclomaticComplexity", "PMD.NPathComplexity", "PMD.AvoidThrowingRawExceptionTypes", "PMD.CloseResource",
        "PMD.AvoidDuplicateLiterals", "PMD.ConfusingTernary"})
public class LoriTimeCommand implements CommonCommand {

    private final LoriTimePlugin loriTimePlugin;

    private final LoriTimeLogger log;

    private final Localization localization;

    public LoriTimeCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.loriTimePlugin = plugin;
        this.log = plugin.getLoggerFactory().create(LoriTimeCommand.class);
        this.localization = localization;
    }

    @Override
    public void execute(final CommonSender sender, final String... args) {
        if (!sender.hasPermission("loritime.see")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length <= 1) {
            loriTimePlugin.getScheduler().runAsyncOnce(() -> {
                final LoriTimePlayer targetPlayer;

                if (args.length == 1) {
                    final Optional<UUID> optionalPlayer;
                    try {
                        optionalPlayer = loriTimePlugin.getNameStorage().getUuid(args[0]);
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
                    if (!sender.isConsole()) {
                        targetPlayer = loriTimePlugin.getPlayerConverter().getOnlinePlayer(sender.getUniqueId());
                    } else {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.consoleself")));
                        return;
                    }
                }

                final boolean isTargetSender = targetPlayer.getUniqueId().equals(sender.getUniqueId());
                if (!isTargetSender && !sender.hasPermission("loritime.see.other")) {
                    printUtilityMessage(sender, "message.nopermission");
                    return;
                }

                final long time;
                try {
                    final OptionalLong optionalTime = loriTimePlugin.getTimeStorage().getTime(targetPlayer.getUniqueId());
                    if (optionalTime.isPresent()) {
                        time = optionalTime.getAsLong();
                    } else {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notfound")
                                .replace("[player]", loriTimePlugin.getNameStorage().getName(targetPlayer.getUniqueId()).get())));
                        return;
                    }
                } catch (final StorageException ex) {
                    log.warn("could not load online time", ex);
                    printUtilityMessage(sender, "message.error");
                    return;
                }
                if (isTargetSender) {
                    sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.timeseen.self")
                            .replace("[time]", TimeUtil.formatTime(time, localization))));
                } else {
                    try {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.timeseen.other")
                                .replace("[player]", loriTimePlugin.getNameStorage().getName(targetPlayer.getUniqueId()).get())
                                .replace("[time]", TimeUtil.formatTime(time, localization))));
                    } catch (final StorageException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else {
            printUtilityMessage(sender, "message.command.loritime.usage");
        }
    }

    private void printUtilityMessage(final CommonSender sender, final String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        if (!source.hasPermission("loritime.see.other")) {
            return new ArrayList<>();
        }

        final List<String> namesList = new ArrayList<>();
        try {
            namesList.addAll(loriTimePlugin.getNameStorage().getNameEntries().stream().toList());
        } catch (final StorageException e) {
            log.warn("could not load name entries on tab completion", e);
        }
        if (args.length == 0) {
            return namesList;
        }
        if (args.length == 1) {
            return filterCompletion(namesList, args[0]);
        }
        return new ArrayList<>();
    }

    private List<String> filterCompletion(final List<String> list, final String currentValue) {
        list.removeIf(elem -> !elem.toLowerCase(Locale.ROOT).startsWith(currentValue.toLowerCase(Locale.ROOT)));
        return list;
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
