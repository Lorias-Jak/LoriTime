package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.LoriTimePlugin;
import org.bstats.velocity.Metrics;

public class VelocityMetrics {

    private final LoriTimePlugin loriTimePlugin;

    private final Metrics metrics;

    public VelocityMetrics(LoriTimePlugin loriTimePlugin, Metrics metrics) {
        this.loriTimePlugin = loriTimePlugin;
        this.metrics = metrics;
    }
}
