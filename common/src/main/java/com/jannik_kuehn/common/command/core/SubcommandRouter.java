package com.jannik_kuehn.common.command.core;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

/**
 * Lightweight shared subcommand registry for command implementations.
 *
 * @param <T> handler type
 */
public class SubcommandRouter<T> {

    /**
     * Registered handlers by normalized alias.
     */
    private final Map<String, T> handlers = new LinkedHashMap<>();

    /**
     * Visible primary command names.
     */
    private final Map<String, T> primaryHandlers = new LinkedHashMap<>();

    /**
     * Creates an empty subcommand router.
     */
    public SubcommandRouter() {
        // Default constructor for explicit API shape.
    }

    /**
     * Registers a subcommand.
     *
     * @param handler handler object
     * @param primary primary subcommand name
     * @param aliases aliases for the same handler
     * @return this router
     */
    public SubcommandRouter<T> register(final T handler, final String primary, final String... aliases) {
        primaryHandlers.put(primary, handler);
        handlers.put(normalize(primary), handler);
        for (final String alias : aliases) {
            handlers.put(normalize(alias), handler);
        }
        return this;
    }

    /**
     * Finds a handler by input.
     *
     * @param input subcommand input
     * @return handler when registered
     */
    public Optional<T> find(final String input) {
        if (input == null) {
            return Optional.empty();
        }
        return Optional.ofNullable(handlers.get(normalize(input)));
    }

    /**
     * Completes primary subcommand names.
     *
     * @param prefix current prefix
     * @return matching subcommands
     */
    public List<String> complete(final String prefix) {
        return CommandCompletions.startsWith(List.copyOf(primaryHandlers.keySet()), prefix);
    }

    private String normalize(final String value) {
        return value.toLowerCase(Locale.ROOT);
    }
}
