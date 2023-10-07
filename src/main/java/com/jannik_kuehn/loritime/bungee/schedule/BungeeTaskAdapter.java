package com.jannik_kuehn.loritime.bungee.schedule;

import com.jannik_kuehn.loritime.api.PluginTask;
import net.md_5.bungee.api.scheduler.ScheduledTask;

public class BungeeTaskAdapter implements PluginTask {

    private final ScheduledTask task;

    public BungeeTaskAdapter(ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
