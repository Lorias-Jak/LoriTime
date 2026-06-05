package com.jannik_kuehn.common.api;

import com.jannik_kuehn.common.LoriTimePlugin;
import com.jannik_kuehn.common.service.LoriTimeServiceFactory;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import java.util.Optional;

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
     * Gets the stable public LoriTime service facade.
     *
     * @return the public service facade when LoriTime is initialized.
     */
    public static Optional<LoriTimeService> service() {
        final LoriTimePlugin plugin = loriTimePlugin;
        return plugin == null ? Optional.empty() : Optional.of(LoriTimeServiceFactory.create(plugin));
    }
}
