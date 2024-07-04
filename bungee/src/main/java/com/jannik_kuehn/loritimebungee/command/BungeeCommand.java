package com.jannik_kuehn.loritimebungee.command;

import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimebungee.LoriTimeBungee;
import com.jannik_kuehn.loritimebungee.util.BungeePlayer;
import com.jannik_kuehn.loritimebungee.util.BungeeSender;
import net.kyori.adventure.platform.bungeecord.BungeeAudiences;
import net.md_5.bungee.api.CommandSender;
import net.md_5.bungee.api.connection.ProxiedPlayer;
import net.md_5.bungee.api.plugin.Command;
import net.md_5.bungee.api.plugin.TabExecutor;

public class BungeeCommand extends Command implements TabExecutor {

    private final LoriTimeBungee bungeePlugin;

    private final BungeeAudiences audiences;

    private final CommonCommand command;

    public BungeeCommand(final LoriTimeBungee bungeePlugin, final BungeeAudiences audiences, final CommonCommand command) {
        super(command.getCommandName(), null, command.getAliases().toArray(new String[0]));
        this.bungeePlugin = bungeePlugin;
        this.audiences = audiences;
        this.command = command;

        register();
    }

    @Override
    public void execute(final CommandSender commandSender, final String[] strings) {
        final CommonSender commonSender = getSender(commandSender);
        this.command.execute(commonSender, strings);
    }

    @Override
    public Iterable<String> onTabComplete(final CommandSender commandSender, final String[] strings) {
        final CommonSender commonSender = getSender(commandSender);
        return this.command.handleTabComplete(commonSender, strings);
    }

    private void register() {
        bungeePlugin.getProxy().getPluginManager().registerCommand(bungeePlugin, this);
    }

    private CommonSender getSender(final CommandSender source) {
        if (source instanceof ProxiedPlayer) {
            return new BungeePlayer((ProxiedPlayer) source);
        } else {
            return new BungeeSender(audiences, source);
        }
    }
}
