package com.jannik_kuehn.common.command.core;

import java.util.List;

/**
 * Common command definition.
 *
 * @param name command root
 * @param aliases command aliases
 * @param permission required permission
 * @param executionPolicy execution policy
 * @param handler command handler
 * @param completionProvider completion provider
 */
public record CommandDefinition(String name, List<String> aliases, String permission,
                                CommandExecutionPolicy executionPolicy, CommandHandler handler,
                                CompletionProvider completionProvider) {

    /**
     * Creates an immutable command definition.
     *
     * @param name command root
     * @param aliases command aliases
     * @param permission required permission
     * @param executionPolicy execution policy
     * @param handler command handler
     * @param completionProvider completion provider
     */
    public CommandDefinition {
        aliases = List.copyOf(aliases);
    }
}
