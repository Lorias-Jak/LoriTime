package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.loritimevelocity.LoriTimeVelocity;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

public class VelocityMetrics {
    private final LoriTimeVelocity loriTimeVelocity;

    private final Metrics metrics;

    public VelocityMetrics(final LoriTimeVelocity loriTimeVelocity, final Metrics metrics) {
        this.loriTimeVelocity = loriTimeVelocity;
        this.metrics = metrics;

        addMetricsData();
    }

    private void addMetricsData() {
        metrics.addCustomChart(new SimplePie("multisetup_enabled", this::isMultiSetupEnabled));
        metrics.addCustomChart(new SimplePie("uses_afk", this::isAfkEnabled));
    }

    private String isMultiSetupEnabled() {
        return String.valueOf(loriTimeVelocity.getPlugin().isMultiSetupEnabled());
    }

    private String isAfkEnabled() {
        return String.valueOf(loriTimeVelocity.getPlugin().isAfkEnabled());
    }
}
