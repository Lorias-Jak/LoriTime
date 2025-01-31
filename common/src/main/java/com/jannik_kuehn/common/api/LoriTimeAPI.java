package com.jannik_kuehn.common.api;

import com.jannik_kuehn.common.LoriTimePlugin;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

/**
 * API for the LoriTime plugin.
 */
public final class LoriTimeAPI {
    /**
     * The {@link LoriTimePlugin} instance.
     */
    private static LoriTimePlugin loriTimePlugin;

    /**
     * Private constructor to prevent instantiation.
     */
    private LoriTimeAPI() {
        // Empty
    }

    /**
     * Sets the {@link LoriTimePlugin} instance.
     * Do not use this method, it is only for internal use!
     *
     * @param plugin The {@link LoriTimePlugin} instance.
     */
    @SuppressWarnings("PMD.AvoidSynchronizedStatement")
    @SuppressFBWarnings("EI_EXPOSE_STATIC_REP2")
    public static void setPlugin(final LoriTimePlugin plugin) {
        synchronized (LoriTimeAPI.class) {
            if (loriTimePlugin == null) {
                loriTimePlugin = plugin;
            }
        }
    }

    /**
     * Gets the core class of this plugin.
     * This class is used to access the plugin's main features.
     *
     * @return The {@link LoriTimePlugin} instance.
     */
    public static LoriTimePlugin get() {
        return loriTimePlugin;
    }
}
