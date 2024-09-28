package com.jannik_kuehn.common.api.logger;

import com.jannik_kuehn.common.exception.LoggerException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * A logger with an optional topic.
 */
public class TopicLogger extends Logger {
    /**
     * The topic of this logger.
     */
    private final String topic;

    /**
     * Creates a new {@link TopicLogger} that adds an optional topic.
     *
     * @param parentLogger A reference to the parent {@link Logger} which is used as parent for this logger.
     * @param clazz        The calling class.
     * @param topic        The topic to add or null.
     */
    @SuppressWarnings({"PMD.ConstructorCallsOverridableMethod", "PMD.AvoidThrowingRawExceptionTypes"})
    public TopicLogger(@NotNull final Logger parentLogger, @NotNull final Class<?> clazz, @Nullable final String topic) {
        super(clazz.getCanonicalName(), null);
        try {
            initLogger(parentLogger);
        } catch (final LoggerException e) {
            parentLogger.log(Level.SEVERE, "Failed to initialize logger on creation.", e);
        }
        this.topic = topic == null ? "" : "(" + topic + ") ";
    }

    private void initLogger(final Logger parentLogger) throws LoggerException {
        try {
            setParent(parentLogger);
            setLevel(Level.ALL);
        } catch (final SecurityException e) {
            throw new LoggerException("Failed to initialize logger", e);
        }
    }

    /**
     * Logs a LogRecord to the log with the topic.
     *
     * @param logRecord The record to log.
     */
    @Override
    public void log(@NotNull final LogRecord logRecord) {
        logRecord.setMessage(topic + logRecord.getMessage());
        logRecord.setLoggerName(getName());
        super.log(logRecord);
    }
}
