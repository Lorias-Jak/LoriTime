package com.jannik_kuehn.loritime.bukkit.schedule;

import com.jannik_kuehn.loritime.api.PluginTask;
import org.bukkit.scheduler.BukkitTask;

public class BukkitTaskAdapter implements PluginTask {

    private final BukkitTask task;

    public BukkitTaskAdapter(BukkitTask scheduler) {
        this.task = scheduler;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
