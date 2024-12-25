package com.jannik_kuehn.loritimepaper.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import io.papermc.paper.threadedregions.scheduler.ScheduledTask;

public class PaperTaskAdapter implements PluginTask {

    private final ScheduledTask task;

    public PaperTaskAdapter(final ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
