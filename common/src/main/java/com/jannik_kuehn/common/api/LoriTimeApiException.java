package com.jannik_kuehn.common.api;

import java.io.Serial;

/**
 * Runtime exception thrown when the public LoriTime API cannot complete an operation.
 */
public class LoriTimeApiException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 3577064814577484088L;

    /**
     * Creates a new API exception.
     *
     * @param message the exception message.
     * @param cause   the cause.
     */
    public LoriTimeApiException(final String message, final Throwable cause) {
        super(message, cause);
    }
}
