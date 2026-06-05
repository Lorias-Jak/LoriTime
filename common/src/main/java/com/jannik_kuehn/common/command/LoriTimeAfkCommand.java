package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.command.core.CommandMessages;
import com.jannik_kuehn.common.command.core.PlayerNameCompletions;
import com.jannik_kuehn.common.config.localization.Localization;
import com.jannik_kuehn.common.platform.CommonCommand;
import com.jannik_kuehn.common.platform.CommonPlayerSender;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.common.player.TrackedLoriTimePlayer;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Command that toggles the sender's AFK state.
 */
@SuppressWarnings("PMD.AvoidLiteralsInIfCondition")
public class LoriTimeAfkCommand implements CommonCommand {

    /**
     * LoriTime plugin runtime.
     */
    private final LoriTimePlugin plugin;

    /**
     * Localization provider.
     */
    private final Localization localization;

    /**
     * Creates an AFK command.
     *
     * @param plugin       LoriTime plugin runtime
     * @param localization localization provider
     */
    public LoriTimeAfkCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    /**
     * Toggles the sender's AFK state.
     *
     * @param sender    command sender
     * @param arguments command arguments
     */
    @Override
    public void execute(final CommonSender sender, final String... arguments) {
        if (!sender.hasPermission("loritime.afk")) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.noPermission");
            return;
        }
        if (!(sender instanceof final CommonPlayerSender playerSender)) {
            return;
        }
        plugin.getScheduler().runAsyncOnce(() -> {
            final TrackedLoriTimePlayer player = plugin.getPlayerConverter().getOnlinePlayer(playerSender.getUniqueId());
            plugin.getAfkStatusProvider().switchPlayerAfk(player);
        });
    }

    /**
     * Completes online player names for AFK command input.
     *
     * @param source command sender
     * @param args   command arguments
     * @return completion suggestions
     */
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

    /**
     * Returns AFK command aliases from configuration.
     *
     * @return command aliases
     */
    @Override
    public List<String> getAliases() {
        return plugin.getConfig().getArrayList("command.Afk.alias").stream()
                .filter(item -> item instanceof String)
                .map(item -> (String) item)
                .collect(Collectors.toList());
    }

    /**
     * Returns the primary AFK command name.
     *
     * @return command name
     */
    @Override
    public String getCommandName() {
        return "afk";
    }

}
