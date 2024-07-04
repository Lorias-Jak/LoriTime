package com.jannik_kuehn.loritimebungee.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class BungeeTaskAdapter implements PluginTask {

    private final ScheduledTask task;

    public BungeeTaskAdapter(final ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
