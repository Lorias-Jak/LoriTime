package com.jannik_kuehn.common.command;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.command.core.CommandCompletions;
import com.jannik_kuehn.common.config.localization.Localization;

import java.util.List;

/**
 * Local operational command for the current LoriTime instance.
 */
public class LoriTimeLocalCommand implements CommonCommand {

    /**
     * Number of arguments required by the reload action.
     */
    private static final int RELOAD_ARGUMENT_COUNT = 1;

    /**
     * Argument count while completing the first argument.
     */
    private static final int FIRST_ARGUMENT_COMPLETION_COUNT = 1;

    /**
     * Argument count before any typed input exists.
     */
    private static final int EMPTY_ARGUMENT_COUNT = 0;

    /**
     * First argument index.
     */
    private static final int FIRST_ARGUMENT_INDEX = 0;

    /**
     * LoriTime plugin.
     */
    private final LoriTimePlugin plugin;

    /**
     * Localization.
     */
    private final Localization localization;

    /**
     * Creates a local command.
     *
     * @param plugin LoriTime plugin
     * @param localization localization
     */
    public LoriTimeLocalCommand(final LoriTimePlugin plugin, final Localization localization) {
        this.plugin = plugin;
        this.localization = localization;
    }

    @Override
    public void execute(final CommonSender sender, final String... arguments) {
        if (!sender.hasPermission("loritime.admin")) {
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.nopermission")));
            return;
        }
        if (arguments.length == RELOAD_ARGUMENT_COUNT && "reload".equalsIgnoreCase(arguments[0])) {
            plugin.getLocalization().reloadTranslation();
            plugin.reload();
            sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.reload.success")));
            return;
        }
        sender.sendMessage(localization.formatTextComponent(localization.getRawMessage("message.command.loritimeadmin.reload.usage")));
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        if (!source.hasPermission("loritime.admin")) {
            return List.of();
        }
        if (args.length <= FIRST_ARGUMENT_COMPLETION_COUNT) {
            return CommandCompletions.startsWith(List.of("reload"),
                    args.length == EMPTY_ARGUMENT_COUNT ? "" : args[FIRST_ARGUMENT_INDEX]);
        }
        return List.of();
    }

    @Override
    public List<String> getAliases() {
        return List.of("lts", "ltlocal");
    }

    @Override
    public String getCommandName() {
        return "ltserver";
    }
}
