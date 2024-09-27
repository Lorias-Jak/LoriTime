package com.jannik_kuehn.common.api.logger;

import com.jannik_kuehn.common.LoriTimePlugin;

import java.util.logging.Logger;

/**
 * Factory for LoriTimeLogger instances.
 */
public class LoggerFactory {
    private final LoriTimePlugin plugin;

    public LoggerFactory(final LoriTimePlugin plugin) {
        this.plugin = plugin;
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
            final Logger julLogger = plugin.getServer().getJavaLogger();
            return new JavaUtilLoggerAdapter(julLogger, clazz, topic);
        } else {
            final org.slf4j.Logger slf4jLogger = plugin.getServer().getSl4jLogger();
            return new Slf4jLoggerAdapter(slf4jLogger, topic);
        }
    }

    private boolean isSlf4jAvailable() {
        return plugin.getServer().getSl4jLogger() != null;
    }
}
