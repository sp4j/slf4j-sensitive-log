package io.github.sp4j.sensitivelog.provider;

import ch.qos.logback.classic.LoggerContext;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Test;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

class SensitiveLogServiceProviderTest {

    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef";

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

    @Test
    void sensitiveLoggerInfoAppliesMaskedMarkersEndToEnd() {
        CapturedLogCall captured = new CapturedLogCall();
        SensitiveLogger logger = new SensitiveLogger(capturingLogger(captured));

        logger.info("Full [MASKED] {}", "secret");
        assertEquals("Full [MASKED] {}", captured.format);
        assertEquals("******", captured.arguments[0]);

        logger.info("Both [MASKED_2] {}", "12345678");
        assertEquals("**3456**", captured.arguments[0]);

        logger.info("First [MASKED_FIRST_2] {}", "12345678");
        assertEquals("**345678", captured.arguments[0]);

        logger.info("Last [MASKED_LAST_2] {}", "12345678");
        assertEquals("123456**", captured.arguments[0]);
    }

    @Test
    void sensitiveLoggerInfoAppliesMixedMarkersInSingleMessage() {
        CapturedLogCall captured = new CapturedLogCall();
        SensitiveLogger logger = new SensitiveLogger(capturingLogger(captured));

        logger.info("Public {} masked [MASKED_2] {} encrypted [SENSITIVE] {}", "alice", "12345678", "secret");

        assertEquals("alice", captured.arguments[0]);
        assertEquals("**3456**", captured.arguments[1]);
        assertTrue(captured.arguments[2] instanceof String);
        assertTrue(!"secret".equals(captured.arguments[2]));
    }

    @Test
    void sensitiveLoggerInfoKeepsTrailingThrowableWithMixedMarkers() {
        CapturedLogCall captured = new CapturedLogCall();
        SensitiveLogger logger = new SensitiveLogger(capturingLogger(captured));
        RuntimeException error = new RuntimeException("boom");

        logger.info(
            "Public {} masked [MASKED_2] {} encrypted [SENSITIVE] {}",
            "alice",
            "12345678",
            "secret",
            error
        );

        assertEquals("alice", captured.arguments[0]);
        assertEquals("**3456**", captured.arguments[1]);
        assertTrue(captured.arguments[2] instanceof String);
        assertTrue(!"secret".equals(captured.arguments[2]));
        assertSame(error, captured.arguments[3]);
    }

    @Test
    void sensitiveLoggerAllLevelsApplyMixedMarkersAndKeepThrowable() {
        CapturedLogCall captured = new CapturedLogCall();
        SensitiveLogger logger = new SensitiveLogger(capturingLogger(captured));
        RuntimeException error = new RuntimeException("boom-all-levels");

        String format = "Public {} masked [MASKED_2] {} encrypted [SENSITIVE] {}";
        logger.trace(format, "alice", "12345678", "secret", error);
        assertMixedSanitization(captured, "trace", error);

        logger.debug(format, "alice", "12345678", "secret", error);
        assertMixedSanitization(captured, "debug", error);

        logger.info(format, "alice", "12345678", "secret", error);
        assertMixedSanitization(captured, "info", error);

        logger.warn(format, "alice", "12345678", "secret", error);
        assertMixedSanitization(captured, "warn", error);

        logger.error(format, "alice", "12345678", "secret", error);
        assertMixedSanitization(captured, "error", error);
    }

    @Test
    void sensitiveLoggerAllLevelsApplySingleArgumentMasking() {
        CapturedLogCall captured = new CapturedLogCall();
        SensitiveLogger logger = new SensitiveLogger(capturingLogger(captured));

        logger.trace("Value [MASKED] {}", "secret");
        assertSingleMaskedValue(captured, "trace");

        logger.debug("Value [MASKED] {}", "secret");
        assertSingleMaskedValue(captured, "debug");

        logger.info("Value [MASKED] {}", "secret");
        assertSingleMaskedValue(captured, "info");

        logger.warn("Value [MASKED] {}", "secret");
        assertSingleMaskedValue(captured, "warn");

        logger.error("Value [MASKED] {}", "secret");
        assertSingleMaskedValue(captured, "error");
    }

    @Test
    void sensitiveLoggerAllLevelsApplySingleArgumentSensitiveEncryption() {
        CapturedLogCall captured = new CapturedLogCall();
        SensitiveLogger logger = new SensitiveLogger(capturingLogger(captured));

        logger.trace("Value [SENSITIVE] {}", "secret");
        assertSingleSensitiveValue(captured, "trace");

        logger.debug("Value [SENSITIVE] {}", "secret");
        assertSingleSensitiveValue(captured, "debug");

        logger.info("Value [SENSITIVE] {}", "secret");
        assertSingleSensitiveValue(captured, "info");

        logger.warn("Value [SENSITIVE] {}", "secret");
        assertSingleSensitiveValue(captured, "warn");

        logger.error("Value [SENSITIVE] {}", "secret");
        assertSingleSensitiveValue(captured, "error");
    }

    private boolean hasSensitiveTurboFilter(LoggerContext loggerContext) {
        return loggerContext.getTurboFilterList().stream().anyMatch(filter -> filter instanceof SensitiveLogbackTurboFilter);
    }

    static class LombokUsingClass {
        // This is exactly what Lombok @Slf4j generates for a class-level logger field.
        private static final Logger log = LoggerFactory.getLogger(LombokUsingClass.class);
    }

    private static Logger capturingLogger(final CapturedLogCall captured) {
        InvocationHandler handler = new InvocationHandler() {
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) {
                String name = method.getName();
                if ("getName".equals(name)) {
                    return "captured";
                }
                if (name.startsWith("is") && method.getReturnType() == boolean.class) {
                    return true;
                }
                if ("atInfo".equals(name) || "atDebug".equals(name) || "atWarn".equals(name)
                    || "atError".equals(name) || "atTrace".equals(name)) {
                    throw new UnsupportedOperationException("Builder API is not used in these tests");
                }
                if (isLevelMethod(name) && args != null && args.length >= 2 && args[0] instanceof String) {
                    captured.level = name;
                    captured.format = (String) args[0];
                    captured.arguments = normalizeArguments(args);
                }
                return null;
            }
        };

        return (Logger) Proxy.newProxyInstance(
            SensitiveLogServiceProviderTest.class.getClassLoader(),
            new Class<?>[] {Logger.class},
            handler
        );
    }

    private static Object[] normalizeArguments(Object[] methodArgs) {
        if (methodArgs.length == 2 && methodArgs[1] instanceof Object[]) {
            return (Object[]) methodArgs[1];
        }
        Object[] result = new Object[methodArgs.length - 1];
        for (int i = 1; i < methodArgs.length; i++) {
            result[i - 1] = methodArgs[i];
        }
        return result;
    }

    private static boolean isLevelMethod(String methodName) {
        return "trace".equals(methodName)
            || "debug".equals(methodName)
            || "info".equals(methodName)
            || "warn".equals(methodName)
            || "error".equals(methodName);
    }

    private static void assertMixedSanitization(CapturedLogCall captured, String expectedLevel, Throwable error) {
        assertEquals(expectedLevel, captured.level);
        assertEquals("alice", captured.arguments[0]);
        assertEquals("**3456**", captured.arguments[1]);
        assertTrue(captured.arguments[2] instanceof String);
        assertTrue(!"secret".equals(captured.arguments[2]));
        assertEquals("secret", SensitiveLogCrypto.decryptWithRawKey(TEST_AES_KEY, (String) captured.arguments[2]));
        assertSame(error, captured.arguments[3]);
    }

    private static void assertSingleMaskedValue(CapturedLogCall captured, String expectedLevel) {
        assertEquals(expectedLevel, captured.level);
        assertEquals("Value [MASKED] {}", captured.format);
        assertEquals("******", captured.arguments[0]);
    }

    private static void assertSingleSensitiveValue(CapturedLogCall captured, String expectedLevel) {
        assertEquals(expectedLevel, captured.level);
        assertEquals("Value [SENSITIVE] {}", captured.format);
        assertTrue(captured.arguments[0] instanceof String);
        assertTrue(!"secret".equals(captured.arguments[0]));
        assertEquals("secret", SensitiveLogCrypto.decryptWithRawKey(TEST_AES_KEY, (String) captured.arguments[0]));
    }

    private static final class CapturedLogCall {
        private String level;
        private String format;
        private Object[] arguments;
    }
}

