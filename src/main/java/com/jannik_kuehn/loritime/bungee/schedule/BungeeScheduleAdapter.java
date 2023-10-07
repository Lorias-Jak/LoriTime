package com.jannik_kuehn.loritime.bungee.schedule;

import com.jannik_kuehn.loritime.api.PluginScheduler;
import com.jannik_kuehn.loritime.api.PluginTask;
import com.jannik_kuehn.loritime.bungee.LoriTimeBungee;
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
