package io.github.sp4j.sensitivelog.provider;

import ch.qos.logback.classic.LoggerContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SensitiveLogServiceProviderTest {

    @Test
    void explicitLoggerFactoryReturnsWrappedLogger() {
        Logger logger = LoggerFactory.getLogger("explicit");
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

        assertNotNull(logger);
        if (loggerFactory instanceof LoggerContext) {
            assertInstanceOf(ch.qos.logback.classic.Logger.class, logger);
            assertTrue(hasSensitiveTurboFilter((LoggerContext) loggerFactory));
        } else {
            assertInstanceOf(SensitiveLogger.class, logger);
        }
    }

    @Test
    void loggerIsCachedByName() {
        Logger first = LoggerFactory.getLogger("cache");
        Logger second = LoggerFactory.getLogger("cache");
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

        if (loggerFactory instanceof LoggerContext) {
            assertEquals(first.getName(), second.getName());
        } else {
            assertTrue(first == second);
        }
    }

    @Test
    void lombokEquivalentFieldIsWrappedLogger() throws Exception {
        Field logField = LombokUsingClass.class.getDeclaredField("log");
        logField.setAccessible(true);
        Object logValue = logField.get(null);
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();

        assertNotNull(logValue);
        if (loggerFactory instanceof LoggerContext) {
            assertInstanceOf(ch.qos.logback.classic.Logger.class, logValue);
            assertTrue(hasSensitiveTurboFilter((LoggerContext) loggerFactory));
        } else {
            assertInstanceOf(SensitiveLogger.class, logValue);
        }
        assertEquals(LombokUsingClass.class.getName(), ((Logger) logValue).getName());
    }

    @Test
    void logbackTurboFilterSanitizesArguments() {
        ILoggerFactory loggerFactory = LoggerFactory.getILoggerFactory();
        Assumptions.assumeTrue(loggerFactory instanceof LoggerContext);

        Object[] arguments = new Object[] {"jdbc:mysql://localhost", "user", "secret"};
        SensitiveArgumentSanitizer.sanitizeVarargsInPlace(
            "MySQL connection properties initialized. URL: {}, username: {}, password: [SENSITIVE] {}",
            arguments
        );

        assertEquals("jdbc:mysql://localhost", arguments[0]);
        assertEquals("user", arguments[1]);
        assertTrue(arguments[2] instanceof String);
        assertTrue(!"secret".equals(arguments[2]));
    }

    private boolean hasSensitiveTurboFilter(LoggerContext loggerContext) {
        return loggerContext.getTurboFilterList().stream().anyMatch(filter -> filter instanceof SensitiveLogbackTurboFilter);
    }

    static class LombokUsingClass {
        // This is exactly what Lombok @Slf4j generates for a class-level logger field.
        private static final Logger log = LoggerFactory.getLogger(LombokUsingClass.class);
    }
}

