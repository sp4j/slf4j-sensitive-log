package io.github.sp4j.sensitivelog.provider;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import org.slf4j.ILoggerFactory;
import org.slf4j.LoggerFactory;

public final class SensitiveLogbackConfigurer {

    private static final String LOGBACK_LOGGER_CONTEXT_CLASS = "ch.qos.logback.classic.LoggerContext";
    private static final String LOGBACK_CONTEXT_LISTENER_CLASS = "ch.qos.logback.classic.spi.LoggerContextListener";
    private static final String LOGBACK_TURBO_FILTER_CLASS = "ch.qos.logback.classic.turbo.TurboFilter";
    private static final String LOGBACK_CONTEXT_CLASS = "ch.qos.logback.core.Context";
    private static final String SENSITIVE_CONTEXT_LISTENER_CLASS =
            "io.github.sp4j.sensitivelog.provider.SensitiveLogbackContextListener";
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
            ClassLoader classLoader = loggerFactory.getClass().getClassLoader();
            installContextListenerIfMissing(loggerFactory, classLoader);

            Method getTurboFilterList = loggerFactory.getClass().getMethod("getTurboFilterList");
            Object turboFilterList = getTurboFilterList.invoke(loggerFactory);
            if (turboFilterList instanceof Iterable) {
                for (Object filter : (Iterable<?>) turboFilterList) {
                    if (filter != null && SENSITIVE_TURBO_FILTER_CLASS.equals(filter.getClass().getName())) {
                        return;
                    }
                }
            }

            Class<?> contextClass = Class.forName(LOGBACK_CONTEXT_CLASS, false, classLoader);
            Class<?> turboFilterClass = Class.forName(LOGBACK_TURBO_FILTER_CLASS, false, classLoader);
            Class<?> sensitiveFilterClass = Class.forName(SENSITIVE_TURBO_FILTER_CLASS, true, classLoader);
            Constructor<?> ctor = sensitiveFilterClass.getDeclaredConstructor();
            ctor.setAccessible(true);
            Object filter = ctor.newInstance();

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

    private static void installContextListenerIfMissing(ILoggerFactory loggerFactory, ClassLoader classLoader)
            throws ReflectiveOperationException {
        Method getCopyOfListenerList = loggerFactory.getClass().getMethod("getCopyOfListenerList");
        Object listeners = getCopyOfListenerList.invoke(loggerFactory);
        if (listeners instanceof Iterable) {
            for (Object listener : (Iterable<?>) listeners) {
                if (listener != null && SENSITIVE_CONTEXT_LISTENER_CLASS.equals(listener.getClass().getName())) {
                    return;
                }
            }
        }

        Class<?> listenerInterfaceClass = Class.forName(LOGBACK_CONTEXT_LISTENER_CLASS, false, classLoader);
        Class<?> listenerClass = Class.forName(SENSITIVE_CONTEXT_LISTENER_CLASS, true, classLoader);
        Constructor<?> ctor = listenerClass.getDeclaredConstructor();
        ctor.setAccessible(true);
        Object listener = ctor.newInstance();
        loggerFactory.getClass().getMethod("addListener", listenerInterfaceClass).invoke(loggerFactory, listener);
    }

}


