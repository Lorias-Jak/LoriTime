package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.storage.NameStorage;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.utils.TimeUtil;
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

    public LoriTimeTopCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    @Override
    public void execute(final CommonSender sender, final String... args) {
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
                } catch (final NumberFormatException e) {
                    printUtilityMessage(sender, "message.command.top.nonumber");
                }
            });
            return;
        }
        printUtilityMessage(sender, "message.command.top.usage");
    }

    private void topOutput(final CommonSender sender, final int site) {
        final List<Map.Entry<String, Long>> timeEntriesList;
        final Map<String, Long> rawTimeEntries = new HashMap<>();
        try {
            for (final Map.Entry<String, ?> allEntry : plugin.getTimeStorage().getAllTimeEntries().entrySet()) {
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
        } catch (final StorageException e) {
            throw new RuntimeException(e);
        }
        final int amountOfPlayers = rawTimeEntries.size();

        final int amountOfMaxPages = (int) Math.ceil(amountOfPlayers / PLAYER_AMOUNT_PER_PAGE);
        if (site < 1 || site > amountOfMaxPages) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.top.wrongpage")
                    .replace("[pages]", 1 + " and " + amountOfMaxPages)
            ));
            return;
        }
        printUtilityMessage(sender, "message.command.top.gatheringdata");

        final int minValue;
        final int maxValue;
        final int usedSite;
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
        for (final Long value : rawTimeEntries.values()) {
            totalTimeSum += value;
        }

        sender.sendMessage(localization.formatTextComponentWithoutPrefix(localization.getRawMessage("message.command.top.headline")
                .replace("[dateAndTime]", LocalDate.now() + " " + LocalTime.now())
                .replace("[pages]", usedSite + " / " + amountOfMaxPages)
                .replace("[totalTime]", TimeUtil.formatTime(totalTimeSum, localization))
        ));
        try {
            final NameStorage nameStorage = plugin.getNameStorage();
            int place = minValue;
            for (final Map.Entry<String, Long> topEntry : timeEntriesList.subList(minValue, maxValue)) {
                place++;
                final UUID uuid = UUID.fromString(topEntry.getKey());
                final Optional<String> optionalName = nameStorage.getName(uuid);
                if (optionalName.isEmpty()) {
                    continue;
                }

                final String name = optionalName.get();
                sender.sendMessage(localization.formatTextComponentWithoutPrefix(localization.getRawMessage("message.command.top.user")
                        .replace("[place]", String.valueOf(place))
                        .replace("[player]", name)
                        .replace("[time]", TimeUtil.formatTime(topEntry.getValue(), localization))
                ));
            }
        } catch (final StorageException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        return new ArrayList<>();
    }

    @Override
    public List<String> getAliases() {
        return plugin.getConfig().getArrayList("command.LoriTimeTop.alias").stream()
                .filter(item -> item instanceof String)
                .map(item -> (String) item)
                .collect(Collectors.toList());
    }

    @Override
    public String getCommandName() {
        return "loritimetop";
    }

    private void printUtilityMessage(final CommonSender sender, final String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }
}
