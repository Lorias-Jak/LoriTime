package com.jannik_kuehn.common.api.common;

import java.util.List;

/**
 * Platform-neutral command contract used by Paper and Velocity adapters.
 */
public interface CommonCommand {

    /**
     * Executes the command for a sender.
     *
     * @param sender command sender
     * @param arguments command arguments
     */
    void execute(CommonSender sender, String... arguments);

    /**
     * Builds tab-completion suggestions for a sender.
     *
     * @param source command sender requesting completions
     * @param args command arguments typed so far
     * @return completion suggestions
     */
    List<String> handleTabComplete(CommonSender source, String... args);

    /**
     * Returns alternate labels for this command.
     *
     * @return command aliases
     */
    List<String> getAliases();

    /**
     * Returns the primary command name.
     *
     * @return command name
     */
    String getCommandName();

}
