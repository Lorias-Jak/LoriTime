package com.jannik_kuehn.loritime.bukkit.util;

import com.jannik_kuehn.loritime.api.CommonLogger;

import java.util.logging.Level;
import java.util.logging.Logger;

public class BukkitLogger implements CommonLogger {
    private final Logger logger;

    public BukkitLogger(Logger logger) {
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
