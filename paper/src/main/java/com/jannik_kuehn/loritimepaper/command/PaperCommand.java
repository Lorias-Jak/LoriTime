package com.jannik_kuehn.loritimepaper.command;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import com.jannik_kuehn.common.platform.CommonCommand;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import com.jannik_kuehn.loritimepaper.util.PaperPlayer;
import com.jannik_kuehn.loritimepaper.util.PaperSender;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandMap;
import org.bukkit.command.CommandSender;
import org.bukkit.command.PluginCommand;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.bukkit.plugin.Plugin;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.List;

/**
 * Bukkit command adapter for shared LoriTime commands.
 */
public class PaperCommand implements CommandExecutor, TabExecutor {

    /**
     * The Paper plugin bootstrap.
     */
    private final LoriTimePaper paperPlugin;

    /**
     * The shared command implementation.
     */
    private final CommonCommand command;

    /**
     * The logger instance.
     */
    private final WrappedLogger log;

    /**
     * Creates and registers a Paper command adapter.
     *
     * @param paperPlugin Paper plugin bootstrap
     * @param command     shared command implementation
     */
    public PaperCommand(final LoriTimePaper paperPlugin, final CommonCommand command) {
        this.paperPlugin = paperPlugin;
        this.command = command;
        this.log = paperPlugin.getPlugin().getLoggerFactory().create(PaperCommand.class);

        register();
    }

    /**
     * Dispatches a Bukkit command to the shared command implementation.
     *
     * @param commandSender Bukkit command sender
     * @param command       Bukkit command
     * @param label         command label
     * @param args          command arguments
     * @return true when the command was handled
     */
    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final CommonSender commonSender = getSender(commandSender);
        this.command.execute(commonSender, args);
        return true;
    }

    /**
     * Requests tab completions from the shared command implementation.
     *
     * @param commandSender Bukkit command sender
     * @param command       Bukkit command
     * @param label         command label
     * @param args          command arguments
     * @return completion suggestions
     */
    @Override
    public @Nullable List<String> onTabComplete(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final CommonSender commonSender = getSender(commandSender);
        return this.command.handleTabComplete(commonSender, args);
    }

    private CommonSender getSender(final CommandSender source) {
        if (source instanceof Player) {
            return new PaperPlayer((Player) source);
        } else {
            return new PaperSender(source);
        }
    }

    private CommandMap getCommandMap() {
        return paperPlugin.getServer().getCommandMap();
    }

    @SuppressWarnings("PMD.AvoidAccessibilityAlteration")
    private PluginCommand createPluginCommand(final String name, final Plugin plugin) {
        PluginCommand command = null;
        try {
            final Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            command = constructor.newInstance(name, plugin);
        } catch (InvocationTargetException | IllegalAccessException | InstantiationException
                 | NoSuchMethodException e) {
            log.error("Error while creating the PluginCommand!", e);
        }
        return command;
    }

    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    private void register() {
        try {
            final CommandMap commandMap = getCommandMap();
            final PluginCommand pluginCommand = createPluginCommand(command.getCommandName(), paperPlugin);
            pluginCommand.setAliases(this.command.getAliases());
            pluginCommand.setExecutor(this);
            commandMap.register(command.getCommandName(), pluginCommand);
        } catch (final Exception e) {
            log.error("Error while registering the command '" + command.getCommandName() + "'!", e);
        }
    }
}
