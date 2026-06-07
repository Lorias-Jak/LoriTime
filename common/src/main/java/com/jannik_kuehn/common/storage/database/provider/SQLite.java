package com.jannik_kuehn.common.storage.database.provider;

import com.github.roleplaycauldron.spellbook.core.logger.WrappedLogger;
import org.sqlite.JDBC;

import java.io.File;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

/**
 * SQLite connection provider backed by direct JDBC connections.
 */
public class SQLite implements LoriTimeConnectionProvider {

    /**
     * Default database filename.
     */
    private static final String DEFAULT_DATABASE_NAME = "loritime.db";

    /**
     * The {@link WrappedLogger} instance.
     */
    private final WrappedLogger log;

    /**
     * The file path to the SQLite database.
     */
    private final String databasePath;

    /**
     * The {@link ReadWriteLock} instance used to synchronize access to the database state.
     */
    private final ReadWriteLock stateLock;

    /**
     * The current state of the database connection.
     */
    private boolean isOpen;

    /**
     * Constructs a new SQLite database connection provider.
     *
     * @param log          the logger instance used for logging operations
     * @param databasePath the file path to the SQLite database
     */
    public SQLite(final WrappedLogger log, final String databasePath) {
        this.log = log;
        this.databasePath = databasePath + "/" + DEFAULT_DATABASE_NAME;
        this.stateLock = new ReentrantReadWriteLock();
        this.isOpen = false;
    }

    @Override
    public void open() {
        stateLock.writeLock().lock();
        try {
            if (isOpen) {
                log.error("The SQLite connection is already open!");
                return;
            }

            try {
                Class.forName(JDBC.class.getName());
            } catch (final ClassNotFoundException e) {
                log.error("SQLite JDBC Driver was not loaded!", e);
                return;
            }

            final File databaseFile = new File(databasePath);
            final File parent = databaseFile.getParentFile();
            if (parent != null && !parent.exists() && !parent.mkdirs()) {
                log.error("Could not create SQLite database directory: " + parent.getAbsolutePath());
                return;
            }

            try (Connection ignored = DriverManager.getConnection("jdbc:sqlite:" + databasePath)) {
                this.isOpen = true;
                log.info("Connected to SQLite database at " + databasePath);
            } catch (final SQLException ex) {
                log.error("Could not connect to the SQLite database!", ex);
            }
        } finally {
            stateLock.writeLock().unlock();
        }
    }

    @Override
    public Connection getConnection() {
        stateLock.readLock().lock();
        try {
            if (!isOpen) {
                log.error("The SQLite connection is not open!");
                return null;
            }

            try {
                return DriverManager.getConnection("jdbc:sqlite:" + databasePath);
            } catch (final SQLException e) {
                log.error("Failed to get database connection", e);
                return null;
            }
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public boolean isClosed() {
        stateLock.readLock().lock();
        try {
            return !isOpen;
        } finally {
            stateLock.readLock().unlock();
        }
    }

    @Override
    public void close() {
        stateLock.writeLock().lock();
        try {
            if (!isOpen) {
                log.error("Could not disconnect from the SQLite database, as it already was closed.");
                return;
            }

            this.isOpen = false;
        } finally {
            stateLock.writeLock().unlock();
        }
    }
}
