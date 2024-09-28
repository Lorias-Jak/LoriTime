package com.jannik_kuehn.common.exception;

import java.io.Serial;

/**
 * An exception that is thrown when an error occurs in the storage.
 */
public class StorageException extends Exception {

    @Serial
    private static final long serialVersionUID = 8383351123736507311L;

    /**
     * Creates a new {@link StorageException} instance.
     */
    public StorageException() {
        super();
    }

    /**
     * Creates a new {@link StorageException} instance with a message.
     *
     * @param message The message of the exception.
     */
    public StorageException(final String message) {
        super(message);
    }

    /**
     * Creates a new {@link StorageException} instance with a cause.
     *
     * @param cause The {@link Throwable} that caused the exception.
     */
    public StorageException(final Throwable cause) {
        super(cause);
    }

    /**
     * Creates a new {@link StorageException} instance with a message and a cause.
     *
     * @param message The message of the exception.
     * @param cause   The cause of the exception.
     */
    public StorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
