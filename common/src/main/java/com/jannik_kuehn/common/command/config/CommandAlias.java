package com.jannik_kuehn.common.command.config;

import java.util.List;

/**
 * Configured command name and aliases.
 *
 * @param name command root name
 * @param aliases command aliases
 */
public record CommandAlias(String name, List<String> aliases) {

    /**
     * Creates an immutable command alias record.
     *
     * @param name command root name
     * @param aliases command aliases
     */
    public CommandAlias {
        aliases = List.copyOf(aliases);
    }
}
