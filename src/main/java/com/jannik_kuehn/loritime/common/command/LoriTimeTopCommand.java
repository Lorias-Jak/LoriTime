package com.jannik_kuehn.loritime.common.command;

import com.jannik_kuehn.loritime.api.CommonCommand;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.localization.Localization;
import com.jannik_kuehn.loritime.common.exception.StorageException;
import com.jannik_kuehn.loritime.common.storage.NameStorage;
import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.common.utils.TimeUtil;
import com.velocitypowered.api.command.CommandSource;
import org.joda.time.LocalDate;
import org.joda.time.LocalTime;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

public class LoriTimeTopCommand implements CommonCommand {

    private static final double PLAYER_AMOUNT_PER_PAGE = 8;
    private final LoriTimePlugin plugin;
    private final Localization localization;

    public LoriTimeTopCommand(LoriTimePlugin plugin, Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    @Override
    public void execute(CommonSender sender, String... args) {
        if (!sender.hasPermission("loritime.top")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (args.length < 1) {
            plugin.getScheduler().runAsyncOnce(() -> {
                topOutput(sender, 1);
            });
            return;
        }
        if (args.length == 1) {
            plugin.getScheduler().runAsyncOnce(() -> {
                try {
                    topOutput(sender, Integer.parseInt(args[0]));
                } catch (NumberFormatException e) {
                    printUtilityMessage(sender,"message.command.top.nonumber");
                }
            });
            return;
        }
        printUtilityMessage(sender,"message.command.top.usage");
    }

    private void topOutput(CommonSender sender, int site) {
        List<Map.Entry<String, Long>> timeEntriesList;
        Map<String, Long> rawTimeEntries = new HashMap<>();
        try {
            for (Map.Entry<String, ?> allEntry : plugin.getTimeStorage().getAllTimeEntries().entrySet()) {
                if (allEntry.getValue() instanceof Long) {
                    rawTimeEntries.put(allEntry.getKey(), (Long) allEntry.getValue());
                } else if (allEntry.getValue() instanceof Integer) {
                    rawTimeEntries.put(allEntry.getKey(), ((Integer) allEntry.getValue()).longValue());
                }
            }

            timeEntriesList = rawTimeEntries.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .collect(Collectors.toList());
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
        int amountOfPlayers = rawTimeEntries.size();

        int amountOfMaxPages = (int) Math.ceil(amountOfPlayers / PLAYER_AMOUNT_PER_PAGE);
        if (site < 1 || site > amountOfMaxPages) {
            sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage("message.command.top.wrongpage")
                    .replace("[pages]", 1 + " and " + amountOfMaxPages)
            ));
            return;
        }
        printUtilityMessage(sender,"message.command.top.gatheringdata");

        int minValue;
        int maxValue;
        int usedSite;
        if (site * 8 > amountOfPlayers && site < amountOfMaxPages) {
            usedSite = 1;
            minValue = 0;
            maxValue = amountOfPlayers;
        } else if (site == amountOfMaxPages) {
            usedSite = amountOfMaxPages;
            minValue = amountOfMaxPages * 8 - 8;
            maxValue = amountOfPlayers;
        } else {
            usedSite = site;
            minValue = site * 8 - 8;
            maxValue = site * 8;
        }

        long totalTimeSum = 0;
        for (Long value : rawTimeEntries.values()) {
            totalTimeSum += value;
        }

        sender.sendMessage(localization.formatMiniMessageWithoutPrefix(localization.getRawMessage("message.command.top.headline")
                .replace("[dateAndTime]", LocalDate.now() + " " + LocalTime.now())
                .replace("[pages]", usedSite + " / " + amountOfMaxPages)
                .replace("[totalTime]", TimeUtil.formatTime(totalTimeSum, localization))
        ));
        try {
            NameStorage nameStorage = plugin.getNameStorage();
            int place = minValue;
            for (Map.Entry<String, Long> topEntry : timeEntriesList.subList(minValue, maxValue)) {
                place++;
                UUID uuid = UUID.fromString(topEntry.getKey());
                Optional<String> optionalName = nameStorage.getName(uuid);
                if (optionalName.isEmpty()) {
                    continue;
                }

                String name = optionalName.get();
                sender.sendMessage(localization.formatMiniMessageWithoutPrefix(localization.getRawMessage("message.command.top.user")
                        .replace("[place]", String.valueOf(place))
                        .replace("[player]", name)
                        .replace("[time]", TimeUtil.formatTime(topEntry.getValue(), localization))
                ));
            }
        } catch (StorageException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> handleTabComplete(CommandSource source, String... args) {
        return new ArrayList<>();
    }

    @Override
    public String[] getAliases() {
        return new String[]{"ttop", "lttop", "ltop", "toptimes"};
    }

    @Override
    public String getCommandName() {
        return "loritimetop";
    }

    private void printUtilityMessage(CommonSender sender, String messageKey) {
        sender.sendMessage(localization.formatMiniMessage(localization.getRawMessage(messageKey)));
    }
}