package com.jannik_kuehn.loritimebukkit.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import org.bukkit.scheduler.BukkitTask;

/**
 * A {@link PluginTask} implementation for Bukkit.
 */
public class BukkitTaskAdapter implements PluginTask {
    /**
     * The {@link BukkitTask} instance.
     */
    private final BukkitTask task;

    /**
     * Creates a new {@link BukkitTaskAdapter} instance.
     *
     * @param scheduler The {@link BukkitTask} instance.
     */
    public BukkitTaskAdapter(final BukkitTask scheduler) {
        this.task = scheduler;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
