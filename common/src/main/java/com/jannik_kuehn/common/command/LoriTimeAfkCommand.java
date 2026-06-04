package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonPlayerSender;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.command.core.PlayerNameCompletions;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;

import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"PMD.CommentRequired", "PMD.AvoidLiteralsInIfCondition"})
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
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.noPermission");
            return;
        }
        if (!(sender instanceof CommonPlayerSender playerSender)) {
            return;
        }
        plugin.getScheduler().runAsyncOnce(() -> {
            final TrackedLoriTimePlayer player = plugin.getPlayerConverter().getOnlinePlayer(playerSender.getUniqueId());
            plugin.getAfkStatusProvider().switchPlayerAfk(player);
        });
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        if (args.length == 0) {
            return PlayerNameCompletions.online(plugin, "");
        }
        if (args.length == 1) {
            return PlayerNameCompletions.online(plugin, args[0]);
        }
        return List.of();
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

}
