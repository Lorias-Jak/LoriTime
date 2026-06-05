package com.jannik_kuehn.common.command.profile;

import com.jannik_kuehn.common.command.config.CommandAlias;
import com.jannik_kuehn.common.platform.CommonCommand;
import com.jannik_kuehn.common.platform.CommonSender;

import java.util.List;

/**
 * Applies configured command name and aliases to a command implementation.
 */
public class ConfiguredCommand implements CommonCommand {

    /**
     * Delegate command behavior.
     */
    private final CommonCommand delegate;

    /**
     * Configured alias metadata.
     */
    private final CommandAlias alias;

    /**
     * Creates a configured command wrapper.
     *
     * @param delegate delegate command
     * @param alias    configured metadata
     */
    public ConfiguredCommand(final CommonCommand delegate, final CommandAlias alias) {
        this.delegate = delegate;
        this.alias = alias;
    }

    @Override
    public void execute(final CommonSender sender, final String... arguments) {
        delegate.execute(sender, arguments);
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        return delegate.handleTabComplete(source, args);
    }

    @Override
    public List<String> getAliases() {
        return alias.aliases();
    }

    @Override
    public String getCommandName() {
        return alias.name();
    }
}
