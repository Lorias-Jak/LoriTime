package com.jannik_kuehn.loritime.common.command;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.common.utils.TimeParser;
import com.jannik_kuehn.loritime.common.utils.TimeUtil;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.jannik_kuehn.loritime.api.CommonCommand;
import com.jannik_kuehn.loritime.common.config.localization.Localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;

public class LoriTimeAdminCommand implements CommonCommand {

    private final LoriTimePlugin plugin;
    private final Localization localization;
    private final TimeParser parser;

    public LoriTimeAdminCommand(LoriTimePlugin plugin, Localization localization, TimeParser parser) {
        this.plugin = plugin;
        this.localization = localization;
        this.parser = parser;
    }

    @Override
    public void execute(CommonSender sender, String... args) {
        if (!sender.hasPermission("loritime.admin")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length < 1) {
            printUtilityMessage(sender, "message.command.loritimeadmin.usage");
            return;
        }
        plugin.getScheduler().runAsyncOnce(() -> {
            String[] subCommandArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subCommandArgs, 0, subCommandArgs.length);

            try {
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "set" -> set(sender, subCommandArgs);
                    case "modify", "mod", "add" -> modify(sender, subCommandArgs);
                    case "reset", "delete", "del" -> reset(sender, subCommandArgs);
                    case "reload" -> reload(sender, subCommandArgs);
                    default -> printUtilityMessage(sender, "message.command.loritimeadmin.usage");
                }
            } catch (StorageException e) {
                plugin.getLogger().error("An exception ocurred during LoriTimeAdminCommand!", e);
            }
        });
    }

    @Override
    public List<String> handleTabComplete(CommonSender source, String... args) {
        if (!source.hasPermission("loritime.admin")) {
            return new ArrayList<>();
        }
        if (args.length == 0) {
            ArrayList<String> completions = new ArrayList<>();
            completions.add("set");
            completions.add("modify");
            completions.add("add");
            completions.add("delete");
            completions.add("reload");
            return completions;
        }
        if (args.length == 1) {
            ArrayList<String> completions = new ArrayList<>();
            completions.add("set");
            completions.add("modify");
            completions.add("add");
            completions.add("delete");
            completions.add("reload");
            return filterCompletion(completions, args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            ArrayList<String> completions = new ArrayList<>();
            try {
                completions.addAll(plugin.getNameStorage().getNameEntries().stream().toList());
            } catch (StorageException e) {
                plugin.getLogger().error("Could not load entries from NameStorage for tab completion in LoriTimeAdminCommand!", e);
            }
            return filterCompletion(completions, args[1]);
        }
        return new ArrayList<>();
    }

    private List<String> filterCompletion(final List<String> list, final String currentValue) {
        list.removeIf(elem -> !elem.toLowerCase(Locale.ROOT).startsWith(currentValue.toLowerCase(Locale.ROOT)));
        return list;
    }

    @Override
    public String[] getAliases() {
        return new String[]{"lta", "ltadmin", "loritimea"};
    }

    @Override
    public String getCommandName() {
        return "loritimeadmin";
    }

    private void set(CommonSender sender, String... args) throws StorageException {
        if (args.length < 2) {
            printUtilityMessage(sender, "message.command.loritimeadmin.set.usage");
            return;
        }
        Optional<UUID> optionalUUID = plugin.getNameStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        UUID playerUUID = optionalUUID.get();

        String[] timeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, timeArgs, 0, timeArgs.length);
        OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (optionalTime.isEmpty()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }
        long time = optionalTime.getAsLong();
        String playerName = plugin.getNameStorage().getName(playerUUID).get();

        if (time < 0) {
            sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritimeadmin.set.negativetime")
                    .replace("[player]", playerName)
                    .replace("[time]", TimeUtil.formatTime(time, localization))));
            return;
        }

        long currentTime;
        try {
            OptionalLong optionalCurrentTime = plugin.getTimeStorage().getTime(playerUUID);
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritime.notfound")
                        .replace("[player]", playerName)));
                return;
            }
        } catch (StorageException ex) {
            plugin.getLogger().warning("could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }

        if (currentTime != time) {
            modifyOnlineTime(playerUUID, time - currentTime);
        }
        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritimeadmin.set.success")
                .replace("[player]", playerName)
                .replace("[time]", TimeUtil.formatTime(time, localization))));
    }

    private void modify(CommonSender sender, String... args) throws StorageException {
        if (args.length < 2) {
            printUtilityMessage(sender, "message.command.loritimeadmin.modify.usage");
            return;
        }

        Optional<UUID> optionalUUID = plugin.getNameStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        UUID playerUUID = optionalUUID.get();

        String[] timeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, timeArgs, 0, timeArgs.length);
        OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (!optionalTime.isPresent()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }
        String playerName = plugin.getNameStorage().getName(playerUUID).get();
        long time = optionalTime.getAsLong();
        long currentTime;
        try {
            OptionalLong optionalCurrentTime = plugin.getTimeStorage().getTime(playerUUID);
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritime.notfound")
                        .replace("[player]", playerName)));
                return;
            }
        } catch (StorageException ex) {
            plugin.getLogger().warning("could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }
        if (currentTime + time < 0) {
            sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritimeadmin.modify.negativetimesum")
                    .replace("[player]", playerName)
                    .replace("[time]", TimeUtil.formatTime(time, localization))));
            return;
        }
        if (time != 0) {
            modifyOnlineTime(playerUUID, time);
        }
        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritimeadmin.modify.success")
                .replace("[player]", playerName)
                .replace("[time]", TimeUtil.formatTime(time, localization))));
    }

    private void reset(CommonSender sender, String... args) throws StorageException {
        if (args.length != 1) {
            printUtilityMessage(sender, "message.command.loritimeadmin.reset.usage");
            return;
        }

        Optional<UUID> optionalUUID = plugin.getNameStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        UUID playerUUID = optionalUUID.get();

        String playerName = plugin.getNameStorage().getName(playerUUID).get();
        long currentTime;
        try {
            OptionalLong optionalCurrentTime = plugin.getTimeStorage().getTime(playerUUID);
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritime.notfound")
                        .replace("[player]", playerName)));
                return;
            }
        } catch (StorageException ex) {
            plugin.getLogger().warning("could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }
        if (currentTime != 0) {
            modifyOnlineTime(playerUUID, -currentTime);
        }
        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritimeadmin.reset.success")
                .replace("[player]", playerName)));
    }

    private  void reload(CommonSender sender, String... args) {
        if (args.length > 1) {
            printUtilityMessage(sender, "message.command.loritimeadmin.reload.usage");
            return;
        }
        plugin.getLocalization().reloadTranslation();

        try {
            plugin.reload();
        } catch (StorageException e) {
            plugin.getLogger().error("Could not reload the plugin!", e);
            printUtilityMessage(sender, "message.command.loritimeadmin.reload.issue");
            return;
        }
        printUtilityMessage(sender, "message.command.loritimeadmin.reload.success");
    }

    public void modifyOnlineTime(UUID uuid, final long modifyBy) {
        try {
            plugin.getTimeStorage().addTime(uuid, modifyBy);
        } catch (StorageException ex) {
            plugin.getLogger().error("could not modify online time of " + uuid.toString(), ex);
        }
    }

    private void printMissingUuidMessage(CommonSender sender, String playerName) {
        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritimeadmin.missinguuid")
                .replace("[player]", playerName)));
    }

    private void printNotTimeMessage(CommonSender sender, String... notTime) {
        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.loritimeadmin.nottime")
                .replace("[argument]", String.join(" ", notTime))));
    }

    private void printUtilityMessage(CommonSender sender, String messageKey) {
        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage(messageKey)));
    }

}
