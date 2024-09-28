package com.jannik_kuehn.common.api.logger;

/**
 * Interface for a logger that supports different logging frameworks.
 */
public interface LoriTimeLogger {
    /**
     * Logs a debug message.
     *
     * @param msg the message to log
     */
    void debug(String msg);

    /**
     * Logs an info message.
     *
     * @param msg the message to log
     */
    void info(String msg);

    /**
     * Logs a warning message.
     *
     * @param msg the message to log
     */
    void warn(String msg);

    /**
     * Logs a warning message with a throwable.
     *
     * @param msg    the message to log
     * @param thrown the throwable to log
     */
    void warn(String msg, Throwable thrown);

    /**
     * Logs an error message.
     *
     * @param msg the message to log
     */
    void error(String msg);

    /**
     * Logs an error message with a throwable.
     *
     * @param msg    the message to log
     * @param thrown the throwable to log
     */
    void error(String msg, Throwable thrown);

    /**
     * Reports an exception to the logger.
     *
     * @param thrown the exception to report
     */
    void reportException(Throwable thrown);
}
