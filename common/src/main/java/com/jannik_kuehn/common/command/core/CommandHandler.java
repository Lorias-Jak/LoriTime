package com.jannik_kuehn.common.command.core;

/**
 * Executes a command.
 */
@FunctionalInterface
public interface CommandHandler {

    /**
     * Handles a command invocation.
     *
     * @param context command context
     */
    void handle(CommandContext context);
}
