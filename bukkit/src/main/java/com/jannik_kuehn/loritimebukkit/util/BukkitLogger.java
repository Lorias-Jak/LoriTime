package com.jannik_kuehn.loritimebukkit.util;

import com.jannik_kuehn.common.api.common.CommonLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BukkitLogger implements CommonLogger {
    private final Logger logger;

    public BukkitLogger(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(final String s) {
        logger.info(s);
    }

    @Override
    public void warning(final String s) {
        logger.warning(s);
    }

    @Override
    public void warning(final String s, final Exception e) {
        logger.log(Level.WARNING, s, e);
    }

    @Override
    public void error(final String s, final Exception e) {
        logger.log(Level.SEVERE, s, e);
    }

    @Override
    public void severe(final String s) {
        logger.log(Level.SEVERE, s);
    }
}
