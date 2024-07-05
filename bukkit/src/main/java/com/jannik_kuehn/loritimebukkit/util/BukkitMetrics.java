package com.jannik_kuehn.loritimebukkit.util;

import com.jannik_kuehn.loritimebukkit.LoriTimeBukkit;
import org.bstats.bukkit.Metrics;
import org.bstats.charts.SimplePie;

public class BukkitMetrics {
    private final LoriTimeBukkit loriTimeBukkit;

    private final Metrics metrics;

    public BukkitMetrics(final LoriTimeBukkit loriTimeBukkit, final Metrics metrics) {
        this.loriTimeBukkit = loriTimeBukkit;
        this.metrics = metrics;

        addMetricsData();
    }

    private void addMetricsData() {
        metrics.addCustomChart(new SimplePie("multisetup_mode", this::getMultiSetupMode));
        metrics.addCustomChart(new SimplePie("multisetup_enabled", this::isMultiSetupEnabled));
        metrics.addCustomChart(new SimplePie("uses_afk", this::isAfkEnabled));
    }

    private String getMultiSetupMode() {
        return loriTimeBukkit.getPlugin().getServer().getServerMode();
    }

    private String isMultiSetupEnabled() {
        return String.valueOf(loriTimeBukkit.getPlugin().isMultiSetupEnabled());
    }

    private String isAfkEnabled() {
        return String.valueOf(loriTimeBukkit.getPlugin().isAfkEnabled());
    }
}
