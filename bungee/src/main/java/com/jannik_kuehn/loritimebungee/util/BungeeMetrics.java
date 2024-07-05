package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.loritimebungee.LoriTimeBungee;
import org.bstats.bungeecord.Metrics;
import org.bstats.charts.SimplePie;

public class BungeeMetrics {
    private final LoriTimeBungee loriTimeBungee;

    private final Metrics metrics;

    public BungeeMetrics(final LoriTimeBungee loriTimeBungee, final Metrics metrics) {
        this.loriTimeBungee = loriTimeBungee;
        this.metrics = metrics;

        addMetricsData();
    }

    private void addMetricsData() {
        metrics.addCustomChart(new SimplePie("multisetup_enabled", this::isMultiSetupEnabled));
        metrics.addCustomChart(new SimplePie("uses_afk", this::isAfkEnabled));
    }

    private String isMultiSetupEnabled() {
        return String.valueOf(loriTimeBungee.getPlugin().isMultiSetupEnabled());
    }

    private String isAfkEnabled() {
        return String.valueOf(loriTimeBungee.getPlugin().isAfkEnabled());
    }
}
