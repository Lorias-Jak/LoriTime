package com.jannik_kuehn.loritime.common.exception;

import java.io.IOException;
import java.io.InvalidObjectException;
import java.io.ObjectInputStream;
import java.io.Serial;
import java.util.Objects;

public class UncheckedStorageException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 5799150803340312996L;

    /**
     * Constructs an instance of this class.
     *
     * @param cause the {@code StorageException}
     * @throws NullPointerException if the cause is {@code null}
     */
    public UncheckedStorageException(StorageException cause) {
        super(Objects.requireNonNull(cause));
    }

    /**
     * Constructs an instance of this class.
     *
     * @param message the detail message, can be null
     * @param cause the {@code StorageException}
     * @throws NullPointerException if the cause is {@code null}
     */
    public UncheckedStorageException(String message, StorageException cause) {
        super(message, Objects.requireNonNull(cause));
    }

    /**
     * Returns the cause of this exception.
     *
     * @return the {@code StorageException} which is the cause of this exception.
     */
    @Override
    public StorageException getCause() {
        return (StorageException) super.getCause();
    }

    /**
     * Called to read the object from a stream.
     *
     * @throws InvalidObjectException if the object is invalid or has a cause that is not an {@code StorageException}
     */
    private void readObject(ObjectInputStream objectInputStream) throws IOException, ClassNotFoundException {
        objectInputStream.defaultReadObject();
        Throwable cause = super.getCause();
        if (!(cause instanceof StorageException)) {
            throw new InvalidObjectException("Cause must be an StorageException");
        }
    }
}
