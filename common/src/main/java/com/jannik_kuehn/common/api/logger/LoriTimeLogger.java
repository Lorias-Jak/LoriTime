package com.jannik_kuehn.common.api.logger;

/**
 * Interface for a logger that supports different logging frameworks.
 */
public interface LoriTimeLogger {

    void debug(String msg);

    void info(String msg);

    void warn(String msg);

    void warn(String msg, Throwable thrown);

    void error(String msg);

    void error(String msg, Throwable thrown);

    void reportException(Throwable thrown);
}
