package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeParser;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.OptionalLong;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoriTimeAdminCommand implements CommonCommand {

    private final LoriTimePlugin plugin;

    private final Localization localization;

    private final TimeParser parser;

    public LoriTimeAdminCommand(final LoriTimePlugin plugin, final Localization localization, final TimeParser parser) {
        this.plugin = plugin;
        this.localization = localization;
        this.parser = parser;
    }

    @Override
    public void execute(final CommonSender sender, final String... args) {
        if (!sender.hasPermission("loritime.admin")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length < 1) {
            printUtilityMessage(sender, "message.command.loritimeadmin.usage");
            return;
        }
        plugin.getScheduler().runAsyncOnce(() -> {
            final String[] subCommandArgs = new String[args.length - 1];
            System.arraycopy(args, 1, subCommandArgs, 0, subCommandArgs.length);

            try {
                switch (args[0].toLowerCase(Locale.ROOT)) {
                    case "set" -> set(sender, subCommandArgs);
                    case "modify", "mod", "add" -> modify(sender, subCommandArgs);
                    case "reset", "delete", "del" -> reset(sender, subCommandArgs);
                    case "reload" -> reload(sender, subCommandArgs);
                    default -> printUtilityMessage(sender, "message.command.loritimeadmin.usage");
                }
            } catch (final StorageException e) {
                plugin.getLogger().error("An exception ocurred during LoriTimeAdminCommand!", e);
            }
        });
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        if (!source.hasPermission("loritime.admin")) {
            return new ArrayList<>();
        }
        if (args.length == 0) {
            final ArrayList<String> completions = new ArrayList<>();
            completions.add("set");
            completions.add("modify");
            completions.add("add");
            completions.add("delete");
            completions.add("reload");
            return completions;
        }
        if (args.length == 1) {
            final ArrayList<String> completions = new ArrayList<>();
            completions.add("set");
            completions.add("modify");
            completions.add("add");
            completions.add("delete");
            completions.add("reload");
            return filterCompletion(completions, args[0]);
        }
        if (args.length == 2 && !args[0].equalsIgnoreCase("reload")) {
            final ArrayList<String> completions = new ArrayList<>();
            try {
                completions.addAll(plugin.getNameStorage().getNameEntries().stream().toList());
            } catch (final StorageException e) {
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
    public List<String> getAliases() {
        return plugin.getConfig().getArrayList("command.LoriTimeAdmin.alias").stream()
                .filter(item -> item instanceof String)
                .map(item -> (String) item)
                .collect(Collectors.toList());
    }

    @Override
    public String getCommandName() {
        return "loritimeadmin";
    }

    private void set(final CommonSender sender, final String... args) throws StorageException {
        if (args.length < 2) {
            printUtilityMessage(sender, "message.command.loritimeadmin.set.usage");
            return;
        }
        final Optional<UUID> optionalUUID = plugin.getNameStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        final LoriTimePlayer player = new LoriTimePlayer(optionalUUID.get(), args[0]);

        final String[] timeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, timeArgs, 0, timeArgs.length);
        final OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (optionalTime.isEmpty()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }
        final long time = optionalTime.getAsLong();

        if (time < 0) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.set.negativetime")
                    .replace("[player]", player.getName())
                    .replace("[time]", TimeUtil.formatTime(time, localization))));
            return;
        }

        final long currentTime;
        try {
            final OptionalLong optionalCurrentTime = plugin.getTimeStorage().getTime(player.getUniqueId());
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notfound")
                        .replace("[player]", player.getName())));
                return;
            }
        } catch (final StorageException ex) {
            plugin.getLogger().warning("could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }

        if (currentTime != time) {
            modifyOnlineTime(player.getUniqueId(), time - currentTime);
        }
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.set.success")
                .replace("[player]", player.getName())
                .replace("[time]", TimeUtil.formatTime(time, localization))));
    }

    private void modify(final CommonSender sender, final String... args) throws StorageException {
        if (args.length < 2) {
            printUtilityMessage(sender, "message.command.loritimeadmin.modify.usage");
            return;
        }

        final Optional<UUID> optionalUUID = plugin.getNameStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        final LoriTimePlayer player = new LoriTimePlayer(optionalUUID.get(), args[0]);

        final String[] timeArgs = new String[args.length - 1];
        System.arraycopy(args, 1, timeArgs, 0, timeArgs.length);
        final OptionalLong optionalTime = parser.parseToSeconds(String.join(" ", timeArgs));
        if (!optionalTime.isPresent()) {
            printNotTimeMessage(sender, timeArgs);
            return;
        }

        final long time = optionalTime.getAsLong();
        final long currentTime;
        try {
            final OptionalLong optionalCurrentTime = plugin.getTimeStorage().getTime(player.getUniqueId());
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notfound")
                        .replace("[player]", player.getName())));
                return;
            }
        } catch (final StorageException ex) {
            plugin.getLogger().warning("could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }
        if (currentTime + time < 0) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.modify.negativetimesum")
                    .replace("[player]", player.getName())
                    .replace("[time]", TimeUtil.formatTime(time, localization))));
            return;
        }
        if (time != 0) {
            modifyOnlineTime(player.getUniqueId(), time);
        }
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.modify.success")
                .replace("[player]", player.getName())
                .replace("[time]", TimeUtil.formatTime(time, localization))));
    }

    private void reset(final CommonSender sender, final String... args) throws StorageException {
        if (args.length != 1) {
            printUtilityMessage(sender, "message.command.loritimeadmin.reset.usage");
            return;
        }

        final Optional<UUID> optionalUUID = plugin.getNameStorage().getUuid(args[0]);
        if (optionalUUID.isEmpty()) {
            printMissingUuidMessage(sender, args[0]);
            return;
        }
        final LoriTimePlayer player = new LoriTimePlayer(optionalUUID.get(), args[0]);

        final long currentTime;
        try {
            final OptionalLong optionalCurrentTime = plugin.getTimeStorage().getTime(player.getUniqueId());
            if (optionalCurrentTime.isPresent()) {
                currentTime = optionalCurrentTime.getAsLong();
            } else {
                sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritime.notfound")
                        .replace("[player]", player.getName())));
                return;
            }
        } catch (final StorageException ex) {
            plugin.getLogger().warning("could not load online time", ex);
            printUtilityMessage(sender, "message.error");
            return;
        }
        if (currentTime != 0) {
            modifyOnlineTime(player.getUniqueId(), -currentTime);
        }
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.reset.success")
                .replace("[player]", player.getName())));
    }

    private void reload(final CommonSender sender, final String... args) {
        if (args.length > 1) {
            printUtilityMessage(sender, "message.command.loritimeadmin.reload.usage");
            return;
        }
        plugin.getLocalization().reloadTranslation();

        plugin.reload();
        printUtilityMessage(sender, "message.command.loritimeadmin.reload.success");
    }

    public void modifyOnlineTime(final UUID uuid, final long modifyBy) {
        try {
            plugin.getTimeStorage().addTime(uuid, modifyBy);
        } catch (final StorageException ex) {
            plugin.getLogger().error("could not modify online time of " + uuid.toString(), ex);
        }
    }

    private void printMissingUuidMessage(final CommonSender sender, final String playerName) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.missinguuid")
                .replace("[player]", playerName)));
    }

    private void printNotTimeMessage(final CommonSender sender, final String... notTime) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.nottime")
                .replace("[argument]", String.join(" ", notTime))));
    }

    private void printUtilityMessage(final CommonSender sender, final String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }

}
