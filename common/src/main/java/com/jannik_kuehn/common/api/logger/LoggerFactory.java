package com.jannik_kuehn.common.api.logger;

import com.jannik_kuehn.common.LoriTimePlugin;

import java.util.logging.Logger;

/**
 * Factory for LoriTimeLogger instances.
 */
public class LoggerFactory {
    /**
     * The {@link LoriTimePlugin}.
     */
    private final LoriTimePlugin loriTimePlugin;

    /**
     * Creates a new {@link LoggerFactory} instance.
     *
     * @param loriTimePlugin the {@link LoriTimePlugin}
     */
    public LoggerFactory(final LoriTimePlugin loriTimePlugin) {
        this.loriTimePlugin = loriTimePlugin;
    }

    /**
     * Creates a logger for a given class.
     *
     * @param clazz The class to create a logger for.
     * @return A LoriTimeLogger implementation.
     */
    public LoriTimeLogger create(final Class<?> clazz) {
        return create(clazz, null);
    }

    /**
     * Creates a logger.
     *
     * @param clazz The class to create a logger for.
     * @param topic The optional topic of the logger passed.
     * @return The decorated Logger.
     */
    public LoriTimeLogger create(final Class<?> clazz, final String topic) {
        if (!isSlf4jAvailable()) {
            final org.slf4j.Logger slf4jLogger = loriTimePlugin.getServer().getSl4jLogger();
            return new Slf4jLoggerAdapter(slf4jLogger, topic);
        }
        final Logger julLogger = loriTimePlugin.getServer().getJavaLogger();
        return new JavaUtilLoggerAdapter(julLogger, clazz, topic);
    }

    private boolean isSlf4jAvailable() {
        return loriTimePlugin.getServer().getSl4jLogger() != null;
    }
}
