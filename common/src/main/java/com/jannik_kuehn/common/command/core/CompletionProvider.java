package com.jannik_kuehn.common.command.core;

import java.util.List;

/**
 * Provides command completions.
 */
@FunctionalInterface
public interface CompletionProvider {

    /**
     * Completes a command invocation.
     *
     * @param context command context
     * @return suggestions
     */
    List<String> complete(CommandContext context);
}
