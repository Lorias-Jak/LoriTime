package com.jannik_kuehn.common.service;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.api.LoriTimeService;

/**
 * Creates internal LoriTime service facade instances.
 */
public final class LoriTimeServiceFactory {

    private LoriTimeServiceFactory() {
        // Utility class
    }

    /**
     * Creates the public service facade backed by the given plugin runtime.
     *
     * @param plugin backing plugin runtime
     * @return service facade
     */
    public static LoriTimeService create(final LoriTimePlugin plugin) {
        return new DefaultLoriTimeService(plugin);
    }
}
