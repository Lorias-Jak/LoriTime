package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.loritimebungee.LoriTimeBungee;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;

/**
 * The metrics for the BungeeCord plugin.
 */
public class BungeeMetrics {
    /**
     * The {@link LoriTimeBungee} instance.
     */
    private final LoriTimeBungee loriTimeBungee;

    /**
     * The {@link Metrics} instance.
     */
    private final Metrics metrics;

    /**
     * Creates a new {@link BungeeMetrics} instance.
     *
     * @param loriTimeBungee The {@link LoriTimeBungee} instance.
     * @param metrics        The {@link Metrics} instance.
     */
    public BungeeMetrics(final LoriTimeBungee loriTimeBungee, final Metrics metrics) {
        this.loriTimeBungee = loriTimeBungee;
        this.metrics = metrics;

        addMetricsData();
    }

    private void addMetricsData() {
        metrics.addCustomChart(new SimplePie("multisetup_enabled", this::multiSetupEnabledString));
        metrics.addCustomChart(new SimplePie("uses_afk", this::afkEnabledString));
    }

    private String multiSetupEnabledString() {
        return String.valueOf(loriTimeBungee.getPlugin().isMultiSetupEnabled());
    }

    private String afkEnabledString() {
        return String.valueOf(loriTimeBungee.getPlugin().isAfkEnabled());
    }
}
