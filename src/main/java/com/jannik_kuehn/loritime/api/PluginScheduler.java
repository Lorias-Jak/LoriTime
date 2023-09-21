package com.jannik_kuehn.loritime.api;

/**
 * Representation of a basic scheduler. Decouples the underlying APIs scheduler as interface for adapters.
 */
public interface PluginScheduler {

    /**
     * Execute a task asynchronously.
     *
     * @param task task to run
     * @return task representation
     */
    PluginTask runAsyncOnce(Runnable task);

    /**
     * Execute a task asynchronously after a fixed amount of time.
     *
     * @param delay seconds to delay execution
     * @param task task to run
     * @return task representation
     */
    PluginTask runAsyncOnceLater(long delay, Runnable task);

    /**
     * Execute a task asynchronously every interval seconds. The execution starts after delay seconds.
     *
     * @param delay seconds to delay execution
     * @param interval seconds to sleep inbetween executions
     * @param task task to run
     * @return task representation
     */
    PluginTask scheduleAsync(long delay, long interval, Runnable task);

    /**
     * Execute a task synchronously.
     *
     * @param task task to run
     * @return task representation
     */
    PluginTask scheduleSync(Runnable task);
}
