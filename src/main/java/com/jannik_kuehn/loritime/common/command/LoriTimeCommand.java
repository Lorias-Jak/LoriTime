package com.jannik_kuehn.loritime.common.command;

import com.jannik_kuehn.loritime.api.CommonCommand;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.localization.Localization;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.jannik_kuehn.loritime.common.storage.NameStorage;
import com.jannik_kuehn.loritime.common.storage.TimeStorage;
import com.jannik_kuehn.loritime.common.utils.CommonSender;
import com.jannik_kuehn.loritime.common.utils.TimeUtil;
import com.velocitypowered.api.command.CommandSource;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class LoriTimeCommand implements CommonCommand {

    private final LoriTimePlugin plugin;
    private final Localization localization;
    private final TimeStorage timeStorage;
    private final NameStorage nameStorage;

    public LoriTimeCommand(LoriTimePlugin plugin, Localization localization, TimeStorage timeStorage, NameStorage nameStorage) {
        this.plugin = plugin;
        this.localization = localization;
        this.timeStorage = timeStorage;
        this.nameStorage = nameStorage;
    }

    @Override
    public void execute(CommonSender sender, String... args) {
        if (!sender.hasPermission("onlinetime.see")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length <= 1) {
            plugin.getScheduler().runAsyncOnce(() -> {
                CommonSender senderInstance = sender;
                UUID targetPlayer;

                if (args.length == 1) {
                    Optional<UUID> optionalPlayer;
                    try {
                        optionalPlayer = nameStorage.getUuid(args[0]);
                    } catch (StorageException e) {
                        throw new RuntimeException(e);
                    }
                    if (optionalPlayer.isPresent()) {
                        targetPlayer = optionalPlayer.get();
                    } else {
                        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritime.notfound")
                                .replace("[player]", args[0])));
                        return;
                    }
                } else {
                    if (!sender.isConsole()) {
                        targetPlayer = senderInstance.getUniqueId();
                    } else {
                        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritime.consoleself")));
                        return;
                    }
                }

                boolean isTargetSender = targetPlayer != null && targetPlayer.equals(senderInstance.getUniqueId());
                if (!isTargetSender && !sender.hasPermission("onlinetime.see.other")) {
                    printUtilityMessage(sender, "message.nopermission");
                    return;
                }

                final long time;
                try {
                    OptionalLong optionalTime = timeStorage.getTime(targetPlayer);
                    if (optionalTime.isPresent()) {
                        time = optionalTime.getAsLong();
                    } else {
                        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritime.notfound")
                                .replace("[player]", nameStorage.getName(targetPlayer).get())));
                        return;
                    }
                } catch (StorageException ex) {
                    plugin.getLogger().warning("could not load online time", ex);
                    printUtilityMessage(sender, "message.error");
                    return;
                }
                if (isTargetSender) {
                    sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritime.timeseen.self")
                            .replace("[time]", TimeUtil.formatTime(time, localization))));
                } else {
                    try {
                        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritime.timeseen.other")
                                .replace("[player]", nameStorage.getName(targetPlayer).get())
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
        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage(messageKey)));
    }

    @Override
    public List<String> handleTabComplete(CommandSource source, String... args) {
        if (!source.hasPermission("onlinetime.see")) {
            return new ArrayList<>();
        }
        if (args.length == 1) {
            try {
                List<String> namesList = new ArrayList<>(nameStorage.getEntries().stream().toList());
                return filterCompletion(namesList, args[0]);
            } catch (StorageException e) {
                throw new RuntimeException(e);
            }
        }
        return new ArrayList<>();
    }
    private List<String> filterCompletion(final List<String> list, final String currentValue) {
        list.removeIf(elem -> !elem.toLowerCase(Locale.ROOT).startsWith(currentValue.toLowerCase(Locale.ROOT)));
        return list;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"lt", "lorit", "ltime"};
    }

    @Override
    public String getCommandName() {
        return "loritime";
    }
}
