package io.github.sp4j.sensitivelog.provider;

import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.turbo.TurboFilter;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public final class SensitiveLogbackConfigurer {

    private SensitiveLogbackConfigurer() {
    }

    public static void installIfLogbackPresent() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        if (!(loggerFactory instanceof LoggerContext)) {
            return;
        }

        LoggerContext loggerContext = (LoggerContext) loggerFactory;
        for (TurboFilter filter : loggerContext.getTurboFilterList()) {
            if (filter instanceof SensitiveLogbackTurboFilter) {
                return;
            }
        }

        SensitiveLogbackTurboFilter filter = new SensitiveLogbackTurboFilter();
        filter.setContext(loggerContext);
        filter.start();
        loggerContext.addTurboFilter(filter);
    }
}


