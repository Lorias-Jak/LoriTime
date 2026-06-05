package com.jannik_kuehn.common.scheduler;

/**
 * Representation of a scheduled task
 */
@FunctionalInterface
public interface PluginTask {

    /**
     * Cancel the execution
     */
    void cancel();
}
