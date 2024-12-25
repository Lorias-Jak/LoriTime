package com.jannik_kuehn.loritimepaper.command;

import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.api.logger.LoriTimeLogger;
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

@SuppressWarnings("PMD.CommentRequired")
public class PaperCommand implements CommandExecutor, TabExecutor {

    private final LoriTimePaper paperPlugin;

    private final CommonCommand command;

    private final LoriTimeLogger log;

    public PaperCommand(final LoriTimePaper paperPlugin, final CommonCommand command) {
        this.paperPlugin = paperPlugin;
        this.command = command;
        this.log = paperPlugin.getPlugin().getLoggerFactory().create(PaperCommand.class);

        register();
    }

    @Override
    public boolean onCommand(@NotNull final CommandSender commandSender, @NotNull final Command command, @NotNull final String label, @NotNull final String[] args) {
        final CommonSender commonSender = getSender(commandSender);
        this.command.execute(commonSender, args);
        return true;
    }

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
