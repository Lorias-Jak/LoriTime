package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.exception.StorageException;
import com.jannik_kuehn.common.platform.CommonCommand;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.common.storage.contract.UnifiedStorage;
import com.jannik_kuehn.common.utils.TimeUtil;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Command that prints a paged ranking of stored player online time.
 */
@SuppressWarnings({"PMD.AvoidLiteralsInIfCondition", "PMD.CognitiveComplexity", "PMD.CyclomaticComplexity",
        "PMD.AvoidThrowingRawExceptionTypes", "PMD.CloseResource"})
public class LoriTimeTopCommand implements CommonCommand {

    /**
     * Amount of players per page.
     */
    private static final double PLAYER_AMOUNT_PER_PAGE = 8;

    /**
     * LoriTime plugin instance.
     */
    private final LoriTimePlugin plugin;

    /**
     * Localization provider.
     */
    private final Localization localization;

    /**
     * Creates the top-list command.
     *
     * @param plugin       LoriTime plugin runtime
     * @param localization localization provider
     */
    public LoriTimeTopCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    /**
     * Executes a paged top-list lookup.
     *
     * @param sender command sender
     * @param args   command arguments
     */
    @Override
    public void execute(final CommonSender sender, final String... args) {
        if (!sender.hasPermission("loritime.top")) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.noPermission");
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
                    CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.command.top.nonumber");
                }
            });
            return;
        }
        CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.command.top.usage");
    }

    private void topOutput(final CommonSender sender, final int site) {
        final List<Map.Entry<String, Long>> timeEntriesList;
        final Map<String, Long> rawTimeEntries = new HashMap<>();
        try {
            for (final Map.Entry<String, ?> allEntry : plugin.getStorage().getAllTimeEntries().entrySet()) {
                if (allEntry.getValue() instanceof Long) {
                    rawTimeEntries.put(allEntry.getKey(), (Long) allEntry.getValue());
                } else if (allEntry.getValue() instanceof Integer) {
                    rawTimeEntries.put(allEntry.getKey(), ((Integer) allEntry.getValue()).longValue());
                }
            }

            timeEntriesList = rawTimeEntries.entrySet()
                    .stream()
                    .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                    .toList();
        } catch (final StorageException e) {
            throw new RuntimeException(e);
        }
        final int amountOfPlayers = rawTimeEntries.size();

        final int amountOfMaxPages = (int) Math.ceil(amountOfPlayers / PLAYER_AMOUNT_PER_PAGE);
        if (site < 1 || site > amountOfMaxPages) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.top.wrongPage")
                    .replace("[pages]", 1 + " and " + amountOfMaxPages)
            ));
            return;
        }
        CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.command.top.gatheringData");

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
                .replace("[dateAndTime]", LocalDate.now() + " " + LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm:ss")))
                .replace("[pages]", usedSite + " / " + amountOfMaxPages)
                .replace("[totalTime]", TimeUtil.formatTime(totalTimeSum, localization))
        ));
        try {
            final UnifiedStorage storage = plugin.getStorage();
            int place = minValue;
            for (final Map.Entry<String, Long> topEntry : timeEntriesList.subList(minValue, maxValue)) {
                place++;
                final UUID uuid = UUID.fromString(topEntry.getKey());
                final Optional<String> optionalName = storage.getName(uuid);
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

    /**
     * Completes top-list command arguments.
     *
     * @param source command sender
     * @param args   command arguments
     * @return completion suggestions
     */
    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        return new ArrayList<>();
    }

    /**
     * Returns top-list command aliases from configuration.
     *
     * @return command aliases
     */
    @Override
    public List<String> getAliases() {
        return plugin.getConfig().getArrayList("command.LoriTimeTop.alias").stream()
                .filter(item -> item instanceof String)
                .map(item -> (String) item)
                .collect(Collectors.toList());
    }

    /**
     * Returns the primary top-list command name.
     *
     * @return command name
     */
    @Override
    public String getCommandName() {
        return "loritimetop";
    }
}
