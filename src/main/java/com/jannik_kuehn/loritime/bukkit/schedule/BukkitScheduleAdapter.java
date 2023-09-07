package com.jannik_kuehn.loritime.bukkit.schedule;

import com.jannik_kuehn.loritime.api.PluginScheduler;
import com.jannik_kuehn.loritime.api.PluginTask;
import com.jannik_kuehn.loritime.bukkit.LoriTimeBukkit;
import org.bukkit.scheduler.BukkitScheduler;

public class BukkitScheduleAdapter implements PluginScheduler {

    private final LoriTimeBukkit plugin;
    private final BukkitScheduler scheduler;

    public BukkitScheduleAdapter(LoriTimeBukkit plugin, BukkitScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public PluginTask runAsyncOnce(Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskAsynchronously(plugin, task));
    }

    @Override
    public PluginTask runAsyncOnceLater(long delay, Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskLater(plugin, task, delay * 20));
    }

    @Override
    public PluginTask scheduleAsync(long delay, long interval, Runnable task) {
        return new BukkitTaskAdapter(scheduler.runTaskTimerAsynchronously(plugin, task, delay * 20, interval * 20));
    }
}
