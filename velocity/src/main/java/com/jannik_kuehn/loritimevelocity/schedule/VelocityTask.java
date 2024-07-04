package com.jannik_kuehn.loritimevelocity.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.velocitypowered.api.scheduler.ScheduledTask;

public class VelocityTask implements PluginTask {

    private final ScheduledTask task;

    public VelocityTask(final ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
