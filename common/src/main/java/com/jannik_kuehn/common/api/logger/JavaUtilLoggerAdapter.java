package com.jannik_kuehn.common.api.logger;

import com.jannik_kuehn.common.LoriTimePlugin;

import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Adapter for java.util.logging, using TopicLogger to add topic support.
 */
public class JavaUtilLoggerAdapter implements LoriTimeLogger {

    private final TopicLogger logger;

    public JavaUtilLoggerAdapter(final Logger parentLogger, final Class<?> clazz, final String topic) {
        this.logger = new TopicLogger(parentLogger, clazz, topic);  // TopicLogger is used here
    }

    @Override
    public void debug(final String msg) {
        if (LoriTimePlugin.getInstance().getConfig().getBoolean("general.debug", false)) {
            logger.log(Level.INFO, msg);
        }
    }

    @Override
    public void info(final String msg) {
        logger.log(Level.INFO, msg);
    }

    @Override
    public void warn(final String msg) {
        logger.log(Level.WARNING, msg);
    }

    @Override
    public void warn(final String msg, final Throwable thrown) {
        logger.log(Level.WARNING, msg, thrown);
    }

    @Override
    public void error(final String msg) {
        logger.log(Level.SEVERE, msg);
    }

    @Override
    public void error(final String msg, final Throwable thrown) {
        logger.log(Level.SEVERE, msg, thrown);
    }

    @Override
    public void reportException(final Throwable thrown) {
        final String msg = "This is an exception that should never occur. "
                + "If you don't know why this occurs, please report it to the author.";
        logger.log(Level.SEVERE, msg, thrown);
    }
}
