package com.jannik_kuehn.common.api.logger;

import com.jannik_kuehn.common.LoriTimePlugin;
import org.slf4j.Logger;

/**
 * Adapter for SLF4J logging with optional topic support.
 */
public class Slf4jLoggerAdapter implements LoriTimeLogger {

    private final Logger logger;

    private final String topic;

    public Slf4jLoggerAdapter(final Logger logger, final String topic) {
        this.logger = logger;
        this.topic = (topic == null || topic.isEmpty()) ? "" : "(" + topic + ") ";
    }

    @Override
    public void debug(final String msg) {
        if (LoriTimePlugin.getInstance().getConfig().getBoolean("general.debug", false)) {
            logger.info("{}{}", topic, msg);
        }
    }

    @Override
    public void info(final String msg) {
        logger.info("{}{}", topic, msg);
    }

    @Override
    public void warn(final String msg) {
        logger.warn("{}{}", topic, msg);
    }

    @Override
    public void warn(final String msg, final Throwable thrown) {
        logger.warn("{}{}", topic, msg, thrown);
    }

    @Override
    public void error(final String msg) {
        logger.error("{}{}", topic, msg);
    }

    @Override
    public void error(final String msg, final Throwable thrown) {
        logger.error("{}{}", topic, msg, thrown);
    }

    @Override
    public void reportException(final Throwable thrown) {
        final String msg = "This is an exception that should never occur. "
                + "If you don't know why this occurs, please report it to the author.";
        logger.error("{}" + msg, topic, thrown);
    }
}
