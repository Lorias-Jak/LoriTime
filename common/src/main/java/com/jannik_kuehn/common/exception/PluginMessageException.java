package com.jannik_kuehn.common.exception;

import java.io.Serial;

/**
 * An exception that is thrown when an error occurs in the plugin message.
 */
public class PluginMessageException extends Exception {

    @Serial
    private static final long serialVersionUID = 8_102_211_762_729_579_439L;

    /**
     * Creates a new {@link PluginMessageException} instance.
     */
    public PluginMessageException() {
        super();
    }

    /**
     * Creates a new {@link PluginMessageException} instance with a message.
     *
     * @param message The message of the exception.
     */
    public PluginMessageException(final String message) {
        super(message);
    }

    /**
     * Creates a new {@link PluginMessageException} instance with a cause.
     *
     * @param cause The {@link Throwable} that caused the exception.
     */
    public PluginMessageException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new {@link PluginMessageException} instance with a message and a cause.
     *
     * @param message The message of the exception.
     * @param cause   The cause of the exception.
     */
    public PluginMessageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
