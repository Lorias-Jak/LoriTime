package com.jannik_kuehn.loritime.velocity.command;

import com.jannik_kuehn.loritime.api.common.CommonCommand;
import com.jannik_kuehn.loritime.api.common.CommonSender;
import com.jannik_kuehn.loritime.velocity.LoriTimeVelocity;
import com.jannik_kuehn.loritime.velocity.util.VelocityPlayer;
import com.jannik_kuehn.loritime.velocity.util.VelocitySender;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.List;

public class VelocityCommand implements SimpleCommand {

    private final LoriTimeVelocity velocityPlugin;
    private final CommonCommand command;
    private CommandMeta meta;

    public VelocityCommand(LoriTimeVelocity velocityPlugin, CommonCommand command) {
        this.velocityPlugin = velocityPlugin;
        this.command = command;

        register();
    }

    @Override
    public void execute(Invocation invocation) {
        CommandSource commandSource = invocation.source();
        String[] args = invocation.arguments();
        CommonSender commonSender = getSender(commandSource);

        this.command.execute(commonSender, args);
    }

    public List<String> suggest(final Invocation invocation) {
        CommonSender commandSource = getSender(invocation.source());
        String[] args = invocation.arguments();
        return this.command.handleTabComplete(commandSource, args);
    }

    public void unregisterCommand() {
        velocityPlugin.getProxyServer().getCommandManager().unregister(meta);
        velocityPlugin.getProxyServer().getCommandManager().unregister(command.getCommandName());
    }

    private void register() {
        List<String> aliases = command.getAliases();
        meta = velocityPlugin.getProxyServer().getCommandManager().metaBuilder(command.getCommandName())
                .aliases(aliases.toArray(new String[0]))
                .build();

        velocityPlugin.getProxyServer().getCommandManager().register(meta, this);
    }

    private CommonSender getSender(CommandSource source) {
        if (source instanceof Player) {
            return new VelocityPlayer((Player) source);
        } else {
            return new VelocitySender(velocityPlugin.getPlugin(), source);
        }
    }
}
