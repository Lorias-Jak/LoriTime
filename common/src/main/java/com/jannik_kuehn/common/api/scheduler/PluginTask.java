package com.jannik_kuehn.common.api.scheduler;

/**
 * Representation of a scheduled task
 */
public interface PluginTask {

    /**
     * Cancel the execution
     */
    void cancel();
}
