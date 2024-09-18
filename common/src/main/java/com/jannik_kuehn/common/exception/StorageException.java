package com.jannik_kuehn.common.exception;

import java.io.Serial;

@SuppressWarnings("PMD.CommentRequired")
public class StorageException extends Exception {

    @Serial
    private static final long serialVersionUID = 8383351123736507311L;

    public StorageException() {
        super();
    }

    public StorageException(final String message) {
        super(message);
    }

    public StorageException(final Throwable cause) {
        super(cause);
    }

    public StorageException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
