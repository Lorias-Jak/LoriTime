package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.loritimevelocity.LoriTimeVelocity;
import org.bstats.charts.SimplePie;
import org.bstats.velocity.Metrics;

/**
 * The metrics for the BungeeCord plugin.
 */
public class VelocityMetrics {
    /**
     * The {@link LoriTimeVelocity} instance.
     */
    private final LoriTimeVelocity loriTimeVelocity;

    /**
     * The {@link Metrics} instance.
     */
    private final Metrics metrics;

    /**
     * Creates a new {@link VelocityMetrics} instance.
     *
     * @param loriTimeVelocity The {@link LoriTimeVelocity} instance.
     * @param metrics          The {@link Metrics} instance.
     */
    public VelocityMetrics(final LoriTimeVelocity loriTimeVelocity, final Metrics metrics) {
        this.loriTimeVelocity = loriTimeVelocity;
        this.metrics = metrics;

        addMetricsData();
    }

    private void addMetricsData() {
        metrics.addCustomChart(new SimplePie("multisetup_enabled", this::multiSetupEnabledString));
        metrics.addCustomChart(new SimplePie("uses_afk", this::afkEnabledString));
    }

    private String multiSetupEnabledString() {
        return String.valueOf(loriTimeVelocity.getPlugin().isMultiSetupEnabled());
    }

    private String afkEnabledString() {
        return String.valueOf(loriTimeVelocity.getPlugin().isAfkEnabled());
    }
}
