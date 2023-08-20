package com.jannik_kuehn.loritime.velocity.module.schedule;

import com.jannik_kuehn.loritime.api.PluginTask;
import com.velocitypowered.api.scheduler.ScheduledTask;

public class VelocityTask implements PluginTask {

    private final ScheduledTask task;

    public VelocityTask(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
