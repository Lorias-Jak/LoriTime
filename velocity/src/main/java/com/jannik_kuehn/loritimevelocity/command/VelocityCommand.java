package com.jannik_kuehn.loritimevelocity.command;

import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.loritimevelocity.LoriTimeVelocity;
import com.jannik_kuehn.loritimevelocity.util.VelocityPlayer;
import com.jannik_kuehn.loritimevelocity.util.VelocitySender;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.List;

@SuppressWarnings("PMD.CommentRequired")
public class VelocityCommand implements SimpleCommand {

    private final LoriTimeVelocity velocityPlugin;

    private final CommonCommand command;

    private CommandMeta meta;

    public VelocityCommand(final LoriTimeVelocity velocityPlugin, final CommonCommand command) {
        this.velocityPlugin = velocityPlugin;
        this.command = command;

        register();
    }

    @Override
    public void execute(final Invocation invocation) {
        final CommandSource commandSource = invocation.source();
        final String[] args = invocation.arguments();
        final CommonSender commonSender = getSender(commandSource);

        this.command.execute(commonSender, args);
    }

    @Override
    public List<String> suggest(final Invocation invocation) {
        final CommonSender commandSource = getSender(invocation.source());
        final String[] args = invocation.arguments();
        return this.command.handleTabComplete(commandSource, args);
    }

    public void unregisterCommand() {
        velocityPlugin.getProxyServer().getCommandManager().unregister(meta);
        velocityPlugin.getProxyServer().getCommandManager().unregister(command.getCommandName());
    }

    private void register() {
        final List<String> aliases = command.getAliases();
        meta = velocityPlugin.getProxyServer().getCommandManager().metaBuilder(command.getCommandName())
                .aliases(aliases.toArray(new String[0]))
                .build();

        velocityPlugin.getProxyServer().getCommandManager().register(meta, this);
    }

    private CommonSender getSender(final CommandSource source) {
        if (source instanceof Player) {
            return new VelocityPlayer((Player) source);
        } else {
            return new VelocitySender(source);
        }
    }
}
