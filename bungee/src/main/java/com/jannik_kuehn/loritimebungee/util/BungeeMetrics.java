package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.common.LoriTimePlugin;
import org.bstats.bungeecord.Metrics;

public class BungeeMetrics {

    private final LoriTimePlugin loriTimePlugin;

    private final Metrics metrics;

    public BungeeMetrics(LoriTimePlugin loriTimePlugin, Metrics metrics) {
        this.loriTimePlugin = loriTimePlugin;
        this.metrics = metrics;
    }
}
