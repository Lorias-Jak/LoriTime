package com.jannik_kuehn.common.exception;

import java.io.Serial;

/**
 * An exception that is thrown when an error occurs in the logger.
 */
public class LoggerException extends Exception {

    @Serial
    private static final long serialVersionUID = -6_096_992_233_776_073_907L;

    /**
     * Creates a new {@link LoggerException} instance.
     */
    public LoggerException() {
        super();
    }

    /**
     * Creates a new {@link LoggerException} instance with a message.
     *
     * @param message The message of the exception.
     */
    public LoggerException(final String message) {
        super(message);
    }

    /**
     * Creates a new {@link LoggerException} instance with a cause.
     *
     * @param cause The {@link Throwable} that caused the exception.
     */
    public LoggerException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new {@link LoggerException} instance with a message and a cause.
     *
     * @param message The message of the exception.
     * @param cause   The cause of the exception.
     */
    public LoggerException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
