package com.jannik_kuehn.common.exception;

import java.io.Serial;

/**
 * An exception that is thrown when an error occurs during the creation or editing of Configuration files.
 */
public class ConfigurationException extends Exception {

    @Serial
    private static final long serialVersionUID = 6778070532057466275L;

    /**
     * Creates a new {@link ConfigurationException} instance.
     */
    public ConfigurationException() {
        super();
    }

    /**
     * Creates a new {@link ConfigurationException} instance with a message.
     *
     * @param message The message of the exception.
     */
    public ConfigurationException(final String message) {
        super(message);
    }

    /**
     * Creates a new {@link ConfigurationException} instance with a cause.
     *
     * @param cause The {@link Throwable} that caused the exception.
     */
    public ConfigurationException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new {@link ConfigurationException} instance with a message and a cause.
     *
     * @param message The message of the exception.
     * @param cause   The cause of the exception.
     */
    public ConfigurationException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
