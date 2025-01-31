package com.jannik_kuehn.common.exception;

import java.io.Serial;

/**
 * This exception is thrown if any problem occurred while updating the plugin.
 */
public class UpdateException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = -3481167413768422470L;

    /**
     * Creates a new {@link UpdateException} instance.
     */
    public UpdateException() {
        super();
    }

    /**
     * Creates a new {@link UpdateException} instance with a message.
     *
     * @param message The message of the exception.
     */
    public UpdateException(final String message) {
        super(message);
    }

    /**
     * Creates a new {@link UpdateException} instance with a cause.
     *
     * @param cause The {@link Throwable} that caused the exception.
     */
    public UpdateException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new {@link UpdateException} instance with a message and a cause.
     *
     * @param message The message of the exception.
     * @param cause   The cause of the exception.
     */
    public UpdateException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
