package com.jannik_kuehn.loritimebungee.util;

import com.jannik_kuehn.common.api.common.CommonLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BungeeLogger implements CommonLogger {

    private final Logger logger;

    public BungeeLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void warning(String s) {
        logger.warning(s);
    }

    @Override
    public void warning(String s, Exception e) {
        logger.log(Level.WARNING, s, e);
    }

    @Override
    public void error(String s, Exception e) {
        logger.log(Level.SEVERE, s, e);
    }

    @Override
    public void severe(String s) {
        logger.log(Level.SEVERE, s);
    }
}
