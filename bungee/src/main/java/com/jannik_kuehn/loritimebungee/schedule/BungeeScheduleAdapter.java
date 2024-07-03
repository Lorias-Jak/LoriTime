package com.jannik_kuehn.loritimebungee.schedule;

import com.jannik_kuehn.loritimebungee.LoriTimeBungee;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import net.md_5.bungee.api.scheduler.TaskScheduler;

public class BungeeScheduleAdapter implements PluginScheduler {

    private final LoriTimeBungee plugin;

    private final TaskScheduler scheduler;

    public BungeeScheduleAdapter(LoriTimeBungee plugin, TaskScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public PluginTask runAsyncOnce(Runnable task) {
        return new BungeeTaskAdapter(scheduler.runAsync(plugin, task));
    }

    @Override
    public PluginTask runAsyncOnceLater(long delay, Runnable task) {
        return new BungeeTaskAdapter(scheduler.schedule(plugin, task, delay, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Override
    public PluginTask scheduleAsync(long delay, long interval, Runnable task) {
        return new BungeeTaskAdapter(scheduler.schedule(plugin, task, delay, interval, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Override
    public PluginTask scheduleSync(Runnable task) {
        return new BungeeTaskAdapter(scheduler.runAsync(plugin, task));
    }
}
