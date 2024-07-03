package com.jannik_kuehn.loritimebukkit.util;

import com.jannik_kuehn.common.LoriTimePlugin;
import org.bstats.bukkit.Metrics;

public class BukkitMetrics {

    private final LoriTimePlugin loriTimePlugin;

    private final Metrics metrics;

    public BukkitMetrics(LoriTimePlugin loriTimePlugin, Metrics metrics) {
        this.loriTimePlugin = loriTimePlugin;
        this.metrics = metrics;
    }
}
