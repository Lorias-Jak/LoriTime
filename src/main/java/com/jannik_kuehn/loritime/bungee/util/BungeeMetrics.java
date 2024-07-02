package com.jannik_kuehn.loritime.bungee.util;

import com.jannik_kuehn.loritime.common.LoriTimePlugin;
import org.bstats.bungeecord.Metrics;

public class BungeeMetrics {

    private final LoriTimePlugin loriTimePlugin;
    private final Metrics metrics;

    public BungeeMetrics(LoriTimePlugin loriTimePlugin, Metrics metrics) {
        this.loriTimePlugin = loriTimePlugin;
        this.metrics = metrics;
    }
}
