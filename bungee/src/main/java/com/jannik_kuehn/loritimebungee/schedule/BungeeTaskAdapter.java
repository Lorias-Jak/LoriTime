package com.jannik_kuehn.loritimebungee.schedule;

import com.jannik_kuehn.common.api.scheduler.PluginTask;
import net.md_5.bungee.api.scheduler.ScheduledTask;

/**
 * An adapter for the {@link PluginTask} interface for BungeeCord.
 */
public class BungeeTaskAdapter implements PluginTask {

    /**
     * The {@link ScheduledTask}.
     */
    private final ScheduledTask task;

    /**
     * Creates a new {@link BungeeTaskAdapter} instance.
     *
     * @param task The {@link ScheduledTask}.
     */
    public BungeeTaskAdapter(final ScheduledTask task) {
        this.task = task;
    }

    @Override
    public void cancel() {
        task.cancel();
    }
}
