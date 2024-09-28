package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimePlayer;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.config.localization.Localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

public class LoriTimeAfkCommand implements CommonCommand {

    private final LoriTimePlugin plugin;

    private final Localization localization;

    public LoriTimeAfkCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    @Override
    public void execute(final CommonSender sender, final String... arguments) {
        if (!sender.hasPermission("loritime.afk")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (sender.isConsole()) {
            return;
        }
        plugin.getScheduler().runAsyncOnce(() -> {
            final LoriTimePlayer player = plugin.getPlayerConverter().getOnlinePlayer(sender.getUniqueId());
            plugin.getAfkStatusProvider().switchPlayerAfk(player);
        });
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        final List<String> result = new ArrayList<>();
        for (final CommonSender onlinePlayer : plugin.getServer().getOnlinePlayers()) {
            result.add(onlinePlayer.getName());
        }
        if (args.length == 0) {
            return result;
        }
        if (args.length == 1) {
            return filterCompletion(result, args[0]);
        }
        return result;
    }

    private List<String> filterCompletion(final List<String> list, final String currentValue) {
        list.removeIf(elem -> !elem.toLowerCase(Locale.ROOT).startsWith(currentValue.toLowerCase(Locale.ROOT)));
        return list;
    }

    @Override
    public List<String> getAliases() {
        return plugin.getConfig().getArrayList("command.Afk.alias").stream()
                .filter(item -> item instanceof String)
                .map(item -> (String) item)
                .collect(Collectors.toList());
    }

    @Override
    public String getCommandName() {
        return "afk";
    }

    private void printUtilityMessage(final CommonSender sender, final String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }
}
