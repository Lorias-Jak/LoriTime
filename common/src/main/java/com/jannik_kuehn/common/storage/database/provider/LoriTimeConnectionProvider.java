package com.jannik_kuehn.common.storage.database.provider;

import com.github.roleplaycauldron.spellbook.database.ConnectionProvider;

import java.io.Closeable;

/**
 * Abstraction for SQL connection providers used by the storage layer.
 */
public interface LoriTimeConnectionProvider extends Closeable, ConnectionProvider {

    /**
     * Opens the underlying connection pool.
     */
    void open();

    /**
     * Returns whether the provider is closed.
     *
     * @return {@code true} if closed
     */
    boolean isClosed();
}
