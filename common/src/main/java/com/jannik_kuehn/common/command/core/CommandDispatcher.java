package com.jannik_kuehn.common.command.core;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.common.CommonCommand;
import com.jannik_kuehn.common.api.common.CommonSender;
import com.jannik_kuehn.common.config.localization.Localization;

import java.util.List;

/**
 * Common dispatcher that applies shared command metadata before executing handlers.
 */
public class CommandDispatcher implements CommonCommand {

    /**
     * LoriTime plugin.
     */
    private final LoriTimePlugin plugin;

    /**
     * Command definition.
     */
    private final CommandDefinition definition;

    /**
     * Localization for common denial messages.
     */
    private final Localization localization;

    /**
     * Creates a command dispatcher.
     *
     * @param plugin LoriTime plugin
     * @param definition command definition
     * @param localization localization
     */
    public CommandDispatcher(final LoriTimePlugin plugin, final CommandDefinition definition, final Localization localization) {
        this.plugin = plugin;
        this.definition = definition;
        this.localization = localization;
    }

    @Override
    public void execute(final CommonSender sender, final String... arguments) {
        if (definition.permission() != null && !definition.permission().isBlank()
                && !sender.hasPermission(definition.permission())) {
            CommandMessages.send(localization, plugin.getLanguageSelector(), sender, "message.noPermission");
            return;
        }
        final CommandContext context = new CommandContext(plugin, sender, arguments);
        if (definition.executionPolicy() == CommandExecutionPolicy.ASYNC) {
            plugin.getScheduler().runAsyncOnce(() -> definition.handler().handle(context));
            return;
        }
        definition.handler().handle(context);
    }

    @Override
    public List<String> handleTabComplete(final CommonSender source, final String... args) {
        if (definition.permission() != null && !definition.permission().isBlank()
                && !source.hasPermission(definition.permission())) {
            return List.of();
        }
        return definition.completionProvider().complete(new CommandContext(plugin, source, args));
    }

    @Override
    public List<String> getAliases() {
        return definition.aliases();
    }

    @Override
    public String getCommandName() {
        return definition.name();
    }
}
