package com.jannik_kuehn.loritimevelocity.schedule;

import com.jannik_kuehn.loritimevelocity.LoriTimeVelocity;
import com.jannik_kuehn.common.api.scheduler.PluginScheduler;
import com.jannik_kuehn.common.api.scheduler.PluginTask;
import com.velocitypowered.api.scheduler.Scheduler;

import java.util.concurrent.TimeUnit;

public class VelocityScheduleAdapter implements PluginScheduler {
    private final LoriTimeVelocity plugin;

    private final Scheduler scheduler;

    public VelocityScheduleAdapter(LoriTimeVelocity plugin, Scheduler scheduler) {
        this.plugin = plugin;
        this.scheduler = scheduler;
    }

    @Override
    public PluginTask runAsyncOnce(Runnable task) {
        return new VelocityTask(
                scheduler.buildTask(plugin, task)
                        .schedule()
        );
    }

    @Override
    public PluginTask runAsyncOnceLater(long delay, Runnable task) {
        return new VelocityTask(
                scheduler.buildTask(plugin, task)
                    .delay(delay, TimeUnit.SECONDS)
                    .schedule()
        );
    }

    @Override
    public PluginTask scheduleAsync(long delay, long interval, Runnable task) {
        return new VelocityTask(
                scheduler.buildTask(plugin, task)
                        .delay(delay, TimeUnit.SECONDS)
                        .repeat(interval, TimeUnit.SECONDS)
                        .schedule()
        );
    }

    @Override
    public PluginTask scheduleSync(Runnable task) {
        return new VelocityTask(
                scheduler.buildTask(plugin, task)
                        .schedule()
        );
    }
}
