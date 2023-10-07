package com.jannik_kuehn.loritime.bungee.command;

import com.jannik_kuehn.loritime.api.CommonCommand;
import com.jannik_kuehn.loritime.api.CommonSender;
import com.jannik_kuehn.loritime.bungee.LoriTimeBungee;
import com.jannik_kuehn.loritime.bungee.util.BungeePlayer;
import com.jannik_kuehn.loritime.bungee.util.BungeeSender;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeCommand extends Command implements TabExecutor {

    private final LoriTimeBungee bungeePlugin;
    private final CommonCommand command;

    public BungeeCommand(LoriTimeBungee bungeePlugin, CommonCommand command) {
        super(command.getCommandName(), null, command.getAliases());
        this.bungeePlugin = bungeePlugin;
        this.command = command;

        register();
    }

    @Override
    public void execute(CommandSender commandSender, String[] strings) {
        CommonSender commonSender = getSender(commandSender);
        this.command.execute(commonSender, strings);
    }

    @Override
    public Iterable<String> onTabComplete(CommandSender commandSender, String[] strings) {
        CommonSender commonSender = getSender(commandSender);
        return this.command.handleTabComplete(commonSender, strings);
    }

    private void register() {
        bungeePlugin.getProxy().getPluginManager().registerCommand(bungeePlugin, this);
    }

    private CommonSender getSender(CommandSender source) {
        if (source instanceof ProxiedPlayer) {
            return new BungeePlayer((ProxiedPlayer) source);
        } else {
            return new BungeeSender((CommandSender) source);
        }
    }
}
