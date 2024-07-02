package com.jannik_kuehn.loritime.bukkit.util;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import org.bstats.bukkit.Metrics;

public class BukkitMetrics {

    private final LoriTimePlugin loriTimePlugin;

    private final Metrics metrics;

    public BukkitMetrics(LoriTimePlugin loriTimePlugin, Metrics metrics) {
        this.loriTimePlugin = loriTimePlugin;
        this.metrics = metrics;
    }
}
