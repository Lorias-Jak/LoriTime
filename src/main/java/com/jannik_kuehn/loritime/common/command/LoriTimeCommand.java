package com.jannik_kuehn.loritime.common.command;

import com.jannik_kuehn.loritime.api.CommonCommand;
import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.localization.Localization;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.common.utils.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoriTimeCommand implements CommonCommand {

    private final LoriTimePlugin plugin;
    private final Localization localization;

    public LoriTimeCommand(LoriTimePlugin plugin, Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    @Override
    public void execute(CommonSender sender, String... args) {
        if (!sender.hasPermission("loritime.see")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length <= 1) {
            plugin.getScheduler().runAsyncOnce(() -> {
                CommonSender senderInstance = sender;
                LoriTimePlayer targetPlayer;

                if (args.length == 1) {
                    Optional<UUID> optionalPlayer;
                    try {
                        optionalPlayer = plugin.getNameStorage().getUuid(args[0]);
                    } catch (StorageException e) {
                        throw new RuntimeException(e);
                    }
                    if (optionalPlayer.isPresent()) {
                        targetPlayer = new LoriTimePlayer(optionalPlayer.get(), args[0]);
                    } else {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notfound")
                                .replace("[player]", args[0])));
                        return;
                    }
                } else {
                    if (!sender.isConsole()) {
                        targetPlayer = new LoriTimePlayer(senderInstance.getUniqueId(), senderInstance.getName());
                    } else {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.consoleself")));
                        return;
                    }
                }

                boolean isTargetSender = targetPlayer.equals(senderInstance.getUniqueId());
                if (!isTargetSender && !sender.hasPermission("loritime.see.other")) {
                    printUtilityMessage(sender, "message.nopermission");
                    return;
                }

                final long time;
                try {
                    OptionalLong optionalTime = plugin.getTimeStorage().getTime(targetPlayer.getUniqueId());
                    if (optionalTime.isPresent()) {
                        time = optionalTime.getAsLong();
                    } else {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notfound")
                                .replace("[player]", plugin.getNameStorage().getName(targetPlayer.getUniqueId()).get())));
                        return;
                    }
                } catch (StorageException ex) {
                    plugin.getLogger().warning("could not load online time", ex);
                    printUtilityMessage(sender, "message.error");
                    return;
                }
                if (isTargetSender) {
                    sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.timeseen.self")
                            .replace("[time]", TimeUtil.formatTime(time, localization))));
                } else {
                    try {
                        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.timeseen.other")
                                .replace("[player]", plugin.getNameStorage().getName(targetPlayer.getUniqueId()).get())
                                .replace("[time]", TimeUtil.formatTime(time, localization))));
                    } catch (StorageException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
        } else {
            printUtilityMessage(sender, "message.command.loritime.usage");
        }
    }

    private void printUtilityMessage(CommonSender sender, String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }

    @Override
    public List<String> handleTabComplete(CommonSender source, String... args) {
        if (!source.hasPermission("loritime.see.other")) {
            return new ArrayList<>();
        }

        List<String> namesList;
        try {
            namesList = new ArrayList<>(plugin.getNameStorage().getNameEntries().stream().toList());
        } catch (StorageException e) {
            namesList = new ArrayList<>();
            plugin.getLogger().error("Could not load entries from NameStorage for tab completion in LoriTimeAdminCommand!", e);
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
        return plugin.getConfig().getArrayList("command.LoriTime.alias").stream()
                .filter(item -> item instanceof String)
                .map(item -> (String) item)
                .collect(Collectors.toList());
    }

    @Override
    public String getCommandName() {
        return "loritime";
    }
}
