package com.jannik_kuehn.loritimevelocity.command;

import com.jannik_kuehn.common.platform.CommonCommand;
import com.jannik_kuehn.common.platform.CommonSender;
import com.jannik_kuehn.loritimevelocity.LoriTimeVelocity;
import com.jannik_kuehn.loritimevelocity.util.VelocityPlayer;
import com.jannik_kuehn.loritimevelocity.util.VelocitySender;
import com.velocitypowered.api.command.CommandMeta;
import com.velocitypowered.api.command.CommandSource;
import com.velocitypowered.api.command.SimpleCommand;
import com.velocitypowered.api.proxy.Player;

import java.util.List;

/**
 * Velocity command adapter for shared LoriTime commands.
 */
public class VelocityCommand implements SimpleCommand {

    /**
     * The Velocity plugin bootstrap.
     */
    private final LoriTimeVelocity velocityPlugin;

    /**
     * The shared command implementation.
     */
    private final CommonCommand command;

    /**
     * The Velocity command metadata.
     */
    private CommandMeta meta;

    /**
     * Creates and registers a Velocity command adapter.
     *
     * @param velocityPlugin Velocity plugin bootstrap
     * @param command        shared command implementation
     */
    public VelocityCommand(final LoriTimeVelocity velocityPlugin, final CommonCommand command) {
        this.velocityPlugin = velocityPlugin;
        this.command = command;

        register();
    }

    /**
     * Dispatches a Velocity invocation to the shared command implementation.
     *
     * @param invocation Velocity command invocation
     */
    @Override
    public void execute(final Invocation invocation) {
        final CommandSource commandSource = invocation.source();
        final String[] args = invocation.arguments();
        final CommonSender commonSender = getSender(commandSource);

        this.command.execute(commonSender, args);
    }

    /**
     * Requests tab completions from the shared command implementation.
     *
     * @param invocation Velocity command invocation
     * @return completion suggestions
     */
    @Override
    public List<String> suggest(final Invocation invocation) {
        final CommonSender commandSource = getSender(invocation.source());
        final String[] args = invocation.arguments();

        return this.command.handleTabComplete(commandSource, args);
    }

    /**
     * Unregisters this command adapter from Velocity.
     */
    public void unregisterCommand() {
        velocityPlugin.getProxyServer().getCommandManager().unregister(meta);
        velocityPlugin.getProxyServer().getCommandManager().unregister(command.getCommandName());
    }

    private void register() {
        final List<String> aliases = command.getAliases();

        meta = velocityPlugin.getProxyServer().getCommandManager()
                .metaBuilder(command.getCommandName())
                .aliases(aliases.toArray(new String[0]))
                .build();

        velocityPlugin.getProxyServer().getCommandManager().register(meta, this);
    }

    private CommonSender getSender(final CommandSource source) {
        if (source instanceof Player) {
            return new VelocityPlayer((Player) source);
        }

        return new VelocitySender(source);
    }
}
