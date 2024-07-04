package com.jannik_kuehn.loritimevelocity.util;

import com.jannik_kuehn.common.api.common.CommonLogger;
import org.slf4j.Logger;

public class VelocityLogger implements CommonLogger {
    private final Logger logger;

    public VelocityLogger(final Logger logger) {
        this.logger = logger;
    }

    @Override
    public void info(final String s) {
        logger.info(s);
    }

    @Override
    public void warning(final String s) {
        logger.warn(s);
    }

    @Override
    public void warning(final String s, final Exception e) {
        logger.warn(s, e);
    }

    @Override
    public void error(final String s, final Exception e) {
        logger.error(s, e);
    }

    @Override
    public void severe(final String s) {
        logger.error(s);
    }
}
