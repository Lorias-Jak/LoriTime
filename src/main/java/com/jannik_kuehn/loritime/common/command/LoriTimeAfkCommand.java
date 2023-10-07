package com.jannik_kuehn.loritime.common.command;

import com.jannik_kuehn.loritime.api.CommonCommand;
import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.api.LoriTimePlayer;
import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import com.jannik_kuehn.loritime.common.config.localization.Localization;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class LoriTimeAfkCommand implements CommonCommand {

    private final LoriTimePlugin plugin;
    private final Localization localization;

    public LoriTimeAfkCommand(LoriTimePlugin plugin, Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    @Override
    public void execute(CommonSender sender, String... arguments) {
        if (!sender.hasPermission("loritime.afk")) {
            printUtilityMessage(sender, "message.nopermission");
            return;
        }
        if (sender.isConsole()) {
            return;
        }
        plugin.getScheduler().runAsyncOnce(() -> {
            LoriTimePlayer player = new LoriTimePlayer(sender.getUniqueId(), sender.getName());
            plugin.getAfkStatusProvider().setPlayerAfk(player);
        });
    }

    @Override
    public List<String> handleTabComplete(CommonSender source, String... args) {
        List<String> result = new ArrayList<>();
        for (CommonSender onlinePlayer : plugin.getServer().getOnlinePlayers()) {
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
    public String[] getAliases() {
        return new String[0];
    }

    @Override
    public String getCommandName() {
        return "afk";
    }

    private void printUtilityMessage(CommonSender sender, String messageKey) {
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage(messageKey)));
    }
}
