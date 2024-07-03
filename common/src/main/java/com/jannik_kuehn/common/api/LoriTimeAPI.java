package com.jannik_kuehn.common.api;

import com.jannik_kuehn.common.LoriTimePlugin;

public class LoriTimeAPI {

    private static LoriTimePlugin loriTimePlugin;

    private LoriTimeAPI() {
        // Empty
    }

    public static void setPlugin(LoriTimePlugin plugin) {
        if (loriTimePlugin == null) {
            loriTimePlugin = plugin;
        }
    }

    public static LoriTimePlugin get() {
        return loriTimePlugin;
    }
}
