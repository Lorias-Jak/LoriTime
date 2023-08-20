package com.jannik_kuehn.loritime.api;

/**
 * Representation of a scheduled task
 */
public interface PluginTask {

    /**
     * Cancel the execution
     */
    void cancel();
}
