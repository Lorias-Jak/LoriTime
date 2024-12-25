package com.jannik_kuehn.loritimepaper.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

/**
 * A {@link PluginTask} implementation for Paper.
 */
public class PaperTaskAdapter implements PluginTask {
    /**
     * The {@link ScheduledTask} instance.
     */
    private final ScheduledTask task;

    /**
     * Creates a new {@link PaperTaskAdapter} instance.
     *
     * @param task The {@link ScheduledTask} instance.
     */
    public PaperTaskAdapter(final ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
