package com.jannik_kuehn.common.api.common;

public interface CommonLogger {
    void info(String s);

    void warning(String s);

    void warning(String s, Exception e);

    void error(String s, Exception e);

    void severe(String s);
}
