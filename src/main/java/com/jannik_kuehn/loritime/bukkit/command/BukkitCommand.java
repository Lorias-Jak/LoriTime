package com.jannik_kuehn.loritime.bukkit.command;

import com.jannik_kuehn.loritime.api.CommonCommand;
import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.bukkit.LoriTimeBukkit;
import com.jannik_kuehn.loritime.bukkit.util.BukkitPlayer;
import com.jannik_kuehn.loritime.bukkit.util.BukkitSender;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabExecutor;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

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

    @SuppressFBWarnings("NP_NULL_ON_SOME_PATH_FROM_RETURN_VALUE")
    private void register() {
        bukkitPlugin.getCommand(command.getCommandName()).setExecutor(this);
        bukkitPlugin.getCommand(command.getCommandName()).setTabCompleter(this);
    }
}
