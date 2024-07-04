package com.jannik_kuehn.loritimebukkit.command;

import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import com.jannik_kuehn.loritimebukkit.util.BukkitPlayer;
import com.jannik_kuehn.loritimebukkit.util.BukkitSender;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
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
import java.lang.reflect.Field;
import java.util.List;

public class BukkitCommand implements CommandExecutor, TabExecutor {

    private final LoriTimeBukkit bukkitPlugin;

    private final CommonCommand command;

    public BukkitCommand(final LoriTimeBukkit bukkitPlugin, final CommonCommand command) {
        this.bukkitPlugin = bukkitPlugin;
        this.command = command;

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
            return new BukkitPlayer((Player) source);
        } else {
            return new BukkitSender(source);
        }
    }

    private CommandMap getCommandMap() {
        try {
            // Zugriff auf die Server-Instanz von Bukkit
            final Object craftServer = Bukkit.getServer();
            // Reflektiere die CraftServer-Klasse (oder eine äquivalente Implementation), um das commandMap-Feld zu erhalten
            final Field commandMapField = craftServer.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(craftServer);
        } catch (final Exception e) {
            bukkitPlugin.getPlugin().getLogger().error("Error while getting the CommandMap!", e);
        }
        return null;
    }

    private PluginCommand createPluginCommand(final String name, final Plugin plugin) {
        PluginCommand command = null;
        try {
            final Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            command = constructor.newInstance(name, plugin);
        } catch (final Exception e) {
            bukkitPlugin.getPlugin().getLogger().error("Error while creating a plugin Command", e);
        }
        return command;
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private void register() {
        final CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            bukkitPlugin.getPlugin().getLogger().severe("Can not register the command '" + command.getCommandName() + "'! Skipping the registration...");
            return;
        }
        final PluginCommand pluginCommand = createPluginCommand(command.getCommandName(), bukkitPlugin);
        pluginCommand.setAliases(this.command.getAliases());
        pluginCommand.setExecutor(this);
        commandMap.register(command.getCommandName(), pluginCommand);
    }
}
