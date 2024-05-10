package com.jannik_kuehn.loritime.bukkit.command;

import com.jannik_kuehn.loritime.api.CommonCommand;
import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.bukkit.LoriTimeBukkit;
import com.jannik_kuehn.loritime.bukkit.util.BukkitPlayer;
import com.jannik_kuehn.loritime.bukkit.util.BukkitSender;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.Bukkit;
import org.bukkit.Server;
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
import java.util.Arrays;
import java.util.List;

public class BukkitCommand implements CommandExecutor, TabExecutor {

    private final LoriTimeBukkit bukkitPlugin;
    private final CommonCommand command;

    public BukkitCommand(LoriTimeBukkit bukkitPlugin, CommonCommand command) {
        this.bukkitPlugin = bukkitPlugin;
        this.command = command;

        register();
    }

    @Override
    public boolean onCommand(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        CommonSender commonSender = getSender(commandSender);
        this.command.execute(commonSender, args);
        return true;
    }

    @Override
    public @Nullable List<String> onTabComplete(@NotNull CommandSender commandSender, @NotNull Command command, @NotNull String label, @NotNull String[] args) {
        CommonSender commonSender = getSender(commandSender);
        return this.command.handleTabComplete(commonSender, args);
    }

    private CommonSender getSender(CommandSender source) {
        if (source instanceof Player) {
            return new BukkitPlayer ((Player) source);
        } else {
            return new BukkitSender(bukkitPlugin.getLoriTimePlugin(), source);
        }
    }

    private CommandMap getCommandMap() {
        try {
            // Zugriff auf die Server-Instanz von Bukkit
            Object craftServer = Bukkit.getServer();
            // Reflektiere die CraftServer-Klasse (oder eine äquivalente Implementation), um das commandMap-Feld zu erhalten
            Field commandMapField = craftServer.getClass().getDeclaredField("commandMap");
            commandMapField.setAccessible(true);
            return (CommandMap) commandMapField.get(craftServer);
        } catch (Exception e) {
            bukkitPlugin.getLoriTimePlugin().getLogger().error("Error while getting the CommandMap!", e);
        }
        return null;
    }

    private PluginCommand createPluginCommand(String name, Plugin plugin) {
        PluginCommand command = null;
        try {
            Constructor<PluginCommand> constructor = PluginCommand.class.getDeclaredConstructor(String.class, Plugin.class);
            constructor.setAccessible(true);
            command = constructor.newInstance(name, plugin);
        } catch (Exception e) {
            bukkitPlugin.getLoriTimePlugin().getLogger().error("Error while creating a plugin Command", e);
        }
        return command;
    }

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private void register() {
        CommandMap commandMap = getCommandMap();
        if (commandMap == null) {
            bukkitPlugin.getLoriTimePlugin().getLogger().severe("Can not register the command '" + command.getCommandName() + "'! Skipping the registration...");
            return;
        }
        PluginCommand pluginCommand = createPluginCommand(command.getCommandName(), bukkitPlugin);
        pluginCommand.setAliases(this.command.getAliases());
        pluginCommand.setExecutor(this);
        commandMap.register(command.getCommandName(), pluginCommand);
    }
}
