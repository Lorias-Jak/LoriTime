package com.jannik_kuehn.loritime.common.utils;

import com.jannik_kuehn.loritime.api.CommonLogger;
import org.slf4j.Logger;

public class LoriTimeLogger implements CommonLogger {
    private final Logger logger;

    public LoriTimeLogger(Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(String s) {
        logger.info(s);
    }

    @Override
    public void warning(String s) {
        logger.warn(s);
    }

    @Override
    public void warning(String s, Exception e) {
        logger.warn(s, e);
    }

    @Override
    public void error(String s, Exception e) {
        logger.error(s, e);
    }

    @Override
    public void severe(String s) {
        logger.error(s);
    }
}
