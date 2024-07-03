package com.jannik_kuehn.common.exception;

import java.io.Serial;

public class StorageException extends Exception {

    @Serial
    private static final long serialVersionUID = 8383351123736507311L;

    public StorageException() {
        super();
    }

    public StorageException(String message) {
        super(message);
    }

    public StorageException(Throwable cause) {
        super(cause);
    }

    public StorageException(String message, Throwable cause) {
        super(message, cause);
    }
}
