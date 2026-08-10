package io.github.sp4j.sensitivelog.provider;

import java.lang.reflect.Method;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public final class SensitiveLogbackConfigurer {

    private static final String LOGBACK_LOGGER_CONTEXT_CLASS = "ch.qos.logback.classic.LoggerContext";
    private static final String LOGBACK_TURBO_FILTER_CLASS = "ch.qos.logback.classic.turbo.TurboFilter";
    private static final String LOGBACK_CONTEXT_CLASS = "ch.qos.logback.core.Context";
    private static final String SENSITIVE_TURBO_FILTER_CLASS =
            "io.github.sp4j.sensitivelog.provider.SensitiveLogbackTurboFilter";

    private SensitiveLogbackConfigurer() {
    }

    public static void installIfLogbackPresent() {
        installIfLogbackPresent(LoggerFactory.getILoggerFactory());
    }

    static void installIfLogbackPresent(ILoggerFactory loggerFactory) {
        if (!isLogbackLoggerContext(loggerFactory)) {
            return;
        }

        try {
            Method getTurboFilterList = loggerFactory.getClass().getMethod("getTurboFilterList");
            Object turboFilterList = getTurboFilterList.invoke(loggerFactory);
            if (turboFilterList instanceof Iterable) {
                for (Object filter : (Iterable<?>) turboFilterList) {
                    if (filter != null && SENSITIVE_TURBO_FILTER_CLASS.equals(filter.getClass().getName())) {
                        return;
                    }
                }
            }

            ClassLoader classLoader = loggerFactory.getClass().getClassLoader();
            Class<?> contextClass = Class.forName(LOGBACK_CONTEXT_CLASS, false, classLoader);
            Class<?> turboFilterClass = Class.forName(LOGBACK_TURBO_FILTER_CLASS, false, classLoader);
            Class<?> sensitiveFilterClass = Class.forName(SENSITIVE_TURBO_FILTER_CLASS, true, classLoader);
            Object filter = sensitiveFilterClass.getDeclaredConstructor().newInstance();

            sensitiveFilterClass.getMethod("setContext", contextClass).invoke(filter, loggerFactory);
            sensitiveFilterClass.getMethod("start").invoke(filter);
            loggerFactory.getClass().getMethod("addTurboFilter", turboFilterClass).invoke(loggerFactory, filter);
        } catch (ReflectiveOperationException e) {
            // Keep logging usable even if Logback internals differ on a given version.
        }
    }

    private static boolean isLogbackLoggerContext(ILoggerFactory loggerFactory) {
        return loggerFactory != null && LOGBACK_LOGGER_CONTEXT_CLASS.equals(loggerFactory.getClass().getName());
    }

}


