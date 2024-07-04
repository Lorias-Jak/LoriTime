package com.jannik_kuehn.loritimebukkit.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import org.bukkit.scheduler.BukkitScheduler;

public class BukkitScheduleAdapter implements PluginScheduler {
    private final LoriTimeBukkit plugin;

    private final BukkitScheduler scheduler;

    public BukkitScheduleAdapter(final LoriTimeBukkit plugin, final BukkitScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public PluginTask runAsyncOnce(final Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskAsynchronously(plugin, task));
    }

    @Override
    public PluginTask runAsyncOnceLater(final long delay, final Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskLater(plugin, task, delay * 20));
    }

    @Override
    public PluginTask scheduleAsync(final long delay, final long interval, final Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskTimerAsynchronously(plugin, task, delay * 20, interval * 20));
    }

    @Override
    public PluginTask scheduleSync(final Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTask(plugin, task));
    }
}
