package com.jannik_kuehn.loritimepaper.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import io.papermc.paper.threadedregions.scheduler.AsyncScheduler;
import io.papermc.paper.threadedregions.scheduler.GlobalRegionScheduler;

import java.util.concurrent.TimeUnit;

/**
 * A {@link PluginScheduler} implementation for Velocity.
 */
public class PaperScheduleAdapter implements PluginScheduler {
    /**
     * The {@link LoriTimePaper} instance.
     */
    private final LoriTimePaper plugin;

    /**
     * The {@link AsyncScheduler} instance.
     */
    private final AsyncScheduler asyncScheduler;

    /**
     * The {@link GlobalRegionScheduler} instance.
     */
    private final GlobalRegionScheduler globalRegionScheduler;

    /**
     * Creates a new {@link PaperScheduleAdapter} instance.
     *
     * @param plugin                The {@link LoriTimePaper} instance.
     * @param asyncScheduler        The {@link AsyncScheduler} instance.
     * @param globalRegionScheduler The {@link GlobalRegionScheduler} instance.
     */
    public PaperScheduleAdapter(final LoriTimePaper plugin, final AsyncScheduler asyncScheduler, final GlobalRegionScheduler globalRegionScheduler) {
        this.plugin = plugin;
        this.asyncScheduler = asyncScheduler;
        this.globalRegionScheduler = globalRegionScheduler;
    }

    @Override
    public PluginTask runAsyncOnce(final Runnable task) {
        return new PaperTaskAdapter(asyncScheduler.runNow(plugin, scheduledTask -> task.run()));
    }

    @Override
    public PluginTask runAsyncOnceLater(final long delay, final Runnable task) {
        return new PaperTaskAdapter(asyncScheduler.runDelayed(plugin,
                scheduledTask -> task.run(),
                delay,
                TimeUnit.SECONDS));
    }

    @Override
    public PluginTask scheduleAsync(final long delay, final long interval, final Runnable task) {
        return new PaperTaskAdapter(asyncScheduler.runAtFixedRate(plugin,
                scheduledTask -> task.run(),
                delay,
                interval,
                TimeUnit.SECONDS));
    }

    @Override
    public PluginTask scheduleSync(final Runnable task) {
        return new PaperTaskAdapter(globalRegionScheduler.run(plugin, scheduledTask -> task.run()));
    }
}
