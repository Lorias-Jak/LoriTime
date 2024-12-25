package com.jannik_kuehn.loritimepaper.util;

import com.jannik_kuehn.loritimepaper.LoriTimePaper;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

/**
 * A class for adding custom metrics data to bStats.
 */
public class BukkitMetrics {
    /**
     * The {@link LoriTimePaper} instance.
     */
    private final LoriTimePaper loriTimePaper;

    /**
     * The {@link Metrics} instance.
     */
    private final Metrics metrics;

    /**
     * Creates a new {@link BukkitMetrics} instance.
     *
     * @param loriTimePaper The {@link LoriTimePaper} instance.
     * @param metrics       The {@link Metrics} instance.
     */
    public BukkitMetrics(final LoriTimePaper loriTimePaper, final Metrics metrics) {
        this.loriTimePaper = loriTimePaper;
        this.metrics = metrics;

        addMetricsData();
    }

    private void addMetricsData() {
        metrics.addCustomChart(new SimplePie("multisetup_mode", this::getMultiSetupMode));
        metrics.addCustomChart(new SimplePie("multisetup_enabled", this::multiSetupEnabledString));
        metrics.addCustomChart(new SimplePie("uses_afk", this::afkEnabledString));
    }

    private String getMultiSetupMode() {
        return loriTimePaper.getPlugin().getServer().getServerMode();
    }

    private String multiSetupEnabledString() {
        return String.valueOf(loriTimePaper.getPlugin().isMultiSetupEnabled());
    }

    private String afkEnabledString() {
        return String.valueOf(loriTimePaper.getPlugin().isAfkEnabled());
    }
}
