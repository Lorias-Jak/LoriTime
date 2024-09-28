package com.jannik_kuehn.loritimebungee.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.loritimebungee.LoriTimeBungee;
import net.md_5.bungee.api.scheduler.TaskScheduler;

/**
 * An adapter for the {@link PluginScheduler} interface for BungeeCord.
 */
public class BungeeScheduleAdapter implements PluginScheduler {
    /**
     * The {@link LoriTimeBungee} instance.
     */
    private final LoriTimeBungee plugin;

    /**
     * The {@link TaskScheduler} instance.
     */
    private final TaskScheduler scheduler;

    /**
     * Creates a new {@link BungeeScheduleAdapter} instance.
     *
     * @param plugin    The {@link LoriTimeBungee} instance.
     * @param scheduler The {@link TaskScheduler} instance.
     */
    public BungeeScheduleAdapter(final LoriTimeBungee plugin, final TaskScheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public PluginTask runAsyncOnce(final Runnable task) {
        return new BungeeTaskAdapter(scheduler.runAsync(plugin, task));
    }

    @Override
    public PluginTask runAsyncOnceLater(final long delay, final Runnable task) {
        return new BungeeTaskAdapter(scheduler.schedule(plugin, task, delay, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Override
    public PluginTask scheduleAsync(final long delay, final long interval, final Runnable task) {
        return new BungeeTaskAdapter(scheduler.schedule(plugin, task, delay, interval, java.util.concurrent.TimeUnit.SECONDS));
    }

    @Override
    public PluginTask scheduleSync(final Runnable task) {
        return new BungeeTaskAdapter(scheduler.runAsync(plugin, task));
    }
}
