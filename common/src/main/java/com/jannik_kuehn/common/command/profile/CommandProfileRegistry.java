package com.jannik_kuehn.common.command.profile;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.command.LoriTimeAdminCommand;
import com.jannik_kuehn.common.command.LoriTimeAfkCommand;
import com.jannik_kuehn.common.command.LoriTimeCommand;
import com.jannik_kuehn.common.command.LoriTimeDebugCommand;
import com.jannik_kuehn.common.command.LoriTimeInfoCommand;
import com.jannik_kuehn.common.command.LoriTimeLocalCommand;
import com.jannik_kuehn.common.command.LoriTimeTopCommand;
import com.jannik_kuehn.common.command.config.CommandAlias;
import com.jannik_kuehn.common.command.config.CommandAliasConfig;
import com.jannik_kuehn.common.command.core.CommandDefinition;
import com.jannik_kuehn.common.command.core.CommandDispatcher;
import com.jannik_kuehn.common.command.core.CommandExecutionPolicy;
import com.jannik_kuehn.common.config.localization.Localization;

import java.util.ArrayList;
import java.util.List;

/**
 * Builds commands for a code-defined runtime command profile.
 */
public class CommandProfileRegistry {

    /**
     * LoriTime plugin.
     */
    private final LoriTimePlugin plugin;

    /**
     * Localization.
     */
    private final Localization localization;

    /**
     * Creates a command profile registry.
     *
     * @param plugin LoriTime plugin
     */
    public CommandProfileRegistry(final LoriTimePlugin plugin) {
        this.plugin = plugin;
        this.localization = plugin.getLocalization();
    }

    /**
     * Builds commands for the selected runtime.
     *
     * @param profile runtime profile
     * @return commands supported by the profile
     */
    public List<CommonCommand> commands(final RuntimeCommandProfile profile) {
        final List<CommonCommand> commands = new ArrayList<>();
        if (profile == RuntimeCommandProfile.PROXY || profile == RuntimeCommandProfile.BACKEND_CANONICAL) {
            commands.add(configured(profile, CommandAliasConfig.CommandNode.ADMIN,
                    new LoriTimeAdminCommand(plugin, localization, plugin.getParser())));
            commands.add(configured(profile, CommandAliasConfig.CommandNode.TIME,
                    new LoriTimeCommand(plugin, localization)));
            commands.add(configured(profile, CommandAliasConfig.CommandNode.TOP,
                    new LoriTimeTopCommand(plugin, localization)));
        }

        commands.add(configured(profile, CommandAliasConfig.CommandNode.LOCAL,
                new LoriTimeLocalCommand(plugin, localization)));
        commands.add(configured(profile, CommandAliasConfig.CommandNode.INFO,
                new LoriTimeInfoCommand(plugin, localization)));
        commands.add(configured(profile, CommandAliasConfig.CommandNode.DEBUG,
                new LoriTimeDebugCommand(plugin, localization)));

        if (profile != RuntimeCommandProfile.PROXY && plugin.isAfkEnabled()) {
            commands.add(configured(profile, CommandAliasConfig.CommandNode.AFK,
                    new LoriTimeAfkCommand(plugin, localization)));
        }
        return commands;
    }

    private CommonCommand configured(final RuntimeCommandProfile profile, final CommandAliasConfig.CommandNode node,
                                     final CommonCommand command) {
        final CommandAlias alias = plugin.getCommandAliasConfig().resolve(profile.aliases(), node,
                command.getCommandName(), command.getAliases());
        return new CommandDispatcher(plugin, new CommandDefinition(alias.name(), alias.aliases(),
                permission(node), CommandExecutionPolicy.SYNC,
                context -> command.execute(context.sender(), context.args()),
                context -> command.handleTabComplete(context.sender(), context.args())),
                localization);
    }

    private String permission(final CommandAliasConfig.CommandNode node) {
        return switch (node) {
            case ADMIN, LOCAL -> "loritime.admin";
            case TIME -> "loritime.see";
            case TOP -> "loritime.top";
            case INFO -> "loritime.info";
            case DEBUG -> "loritime.debug";
            case AFK -> "loritime.afk";
        };
    }
}
