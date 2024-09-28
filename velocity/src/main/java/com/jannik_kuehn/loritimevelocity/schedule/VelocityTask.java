package com.jannik_kuehn.loritimevelocity.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.velocitypowered.api.scheduler.ScheduledTask;

/**
 * A {@link PluginTask} implementation for Velocity.
 */
public class VelocityTask implements PluginTask {
    /**
     * The {@link ScheduledTask} instance.
     */
    private final ScheduledTask task;

    /**
     * Creates a new {@link VelocityTask} instance.
     *
     * @param task The {@link ScheduledTask} instance.
     */
    public VelocityTask(final ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
