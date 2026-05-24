package com.jannik_kuehn.common.command.core;

/**
 * Command execution placement.
 */
public enum CommandExecutionPolicy {
    /**
     * Execute on the current platform command thread.
     */
    SYNC,

    /**
     * Execute through LoriTime's async scheduler.
     */
    ASYNC
}
