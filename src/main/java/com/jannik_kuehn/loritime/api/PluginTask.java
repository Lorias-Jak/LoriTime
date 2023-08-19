package com.jannik_kuehn.loritime.api;

/**
 * Representation of a scheduled task. This may be a task that is run only once as well as a reoccurring task.
 */
public interface PluginTask {

    /**
     * Cancel the execution
     */
    void cancel();
}
