package com.jannik_kuehn.loritimebukkit.util;

import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

/**
 * A class for adding custom metrics data to bStats.
 */
public class BukkitMetrics {
    /**
     * The {@link LoriTimeBukkit} instance.
     */
    private final LoriTimeBukkit loriTimeBukkit;

    /**
     * The {@link Metrics} instance.
     */
    private final Metrics metrics;

    /**
     * Creates a new {@link BukkitMetrics} instance.
     *
     * @param loriTimeBukkit The {@link LoriTimeBukkit} instance.
     * @param metrics        The {@link Metrics} instance.
     */
    public BukkitMetrics(final LoriTimeBukkit loriTimeBukkit, final Metrics metrics) {
        this.loriTimeBukkit = loriTimeBukkit;
        this.metrics = metrics;

        addMetricsData();
    }

    private void addMetricsData() {
        metrics.addCustomChart(new SimplePie("multisetup_mode", this::getMultiSetupMode));
        metrics.addCustomChart(new SimplePie("multisetup_enabled", this::multiSetupEnabledString));
        metrics.addCustomChart(new SimplePie("uses_afk", this::afkEnabledString));
    }

    private String getMultiSetupMode() {
        return loriTimeBukkit.getPlugin().getServer().getServerMode();
    }

    private String multiSetupEnabledString() {
        return String.valueOf(loriTimeBukkit.getPlugin().isMultiSetupEnabled());
    }

    private String afkEnabledString() {
        return String.valueOf(loriTimeBukkit.getPlugin().isAfkEnabled());
    }
}
