package com.jannik_kuehn.loritimebukkit.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import org.bukkit.scheduler.BukkitTask;

public class BukkitTaskAdapter implements PluginTask {

    private final BukkitTask task;

    public BukkitTaskAdapter(final BukkitTask scheduler) {
        this.task = scheduler;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
