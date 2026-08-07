package io.github.sp4j.sensitivelog.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SensitiveLogServiceProviderTest {

    @Test
    void explicitLoggerFactoryReturnsWrappedLogger() {
        Logger logger = LoggerFactory.getLogger("explicit");

        assertNotNull(logger);
        assertInstanceOf(SensitiveLogger.class, logger);
    }

    @Test
    void loggerIsCachedByName() {
        Logger first = LoggerFactory.getLogger("cache");
        Logger second = LoggerFactory.getLogger("cache");

        assertTrue(first == second);
    }

    @Test
    void lombokEquivalentFieldIsWrappedLogger() throws Exception {
        Field logField = LombokUsingClass.class.getDeclaredField("log");
        logField.setAccessible(true);
        Object logValue = logField.get(null);

        assertNotNull(logValue);
        assertInstanceOf(SensitiveLogger.class, logValue);
        assertEquals(LombokUsingClass.class.getName(), ((Logger) logValue).getName());
    }

    static class LombokUsingClass {
        // This is exactly what Lombok @Slf4j generates for a class-level logger field.
        private static final Logger log = LoggerFactory.getLogger(LombokUsingClass.class);
    }
}

