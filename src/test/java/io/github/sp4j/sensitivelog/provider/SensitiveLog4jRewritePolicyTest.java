package io.github.sp4j.sensitivelog.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

import org.apache.logging.log4j.Level;
import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.ParameterizedMessage;
import org.junit.jupiter.api.Test;

class SensitiveLog4jRewritePolicyTest {

    private static final String TEST_AES_KEY = "0123456789abcdef0123456789abcdef";

    @Test
    void rewritesMixedMarkersForPureLog4j2() {
        SensitiveLog4jRewritePolicy policy = SensitiveLog4jRewritePolicy.createPolicy();
        LogEvent event = Log4jLogEvent.newBuilder()
            .setLoggerName("pure-log4j2")
            .setLevel(Level.INFO)
            .setMessage(new ParameterizedMessage(
                "Public {} masked [MASKED_2] {} encrypted [SENSITIVE] {}",
                new Object[] {"alice", "12345678", "secret"}
            ))
            .build();

        LogEvent rewritten = policy.rewrite(event);
        Object[] args = rewritten.getMessage().getParameters();

        assertEquals("alice", args[0]);
        assertEquals("**3456**", args[1]);
        assertNotEquals("secret", args[2]);
        assertEquals("secret", SensitiveLogCrypto.decryptWithRawKey(TEST_AES_KEY, String.valueOf(args[2])));
    }

    @Test
    void keepsTrailingThrowableInPureLog4j2Flow() {
        SensitiveLog4jRewritePolicy policy = SensitiveLog4jRewritePolicy.createPolicy();
        RuntimeException error = new RuntimeException("boom");
        LogEvent event = Log4jLogEvent.newBuilder()
            .setLoggerName("pure-log4j2")
            .setLevel(Level.ERROR)
            .setThrown(error)
            .setMessage(new ParameterizedMessage(
                "Error [SENSITIVE] {}",
                new Object[] {"secret", error},
                error
            ))
            .build();

        LogEvent rewritten = policy.rewrite(event);
        Object[] args = rewritten.getMessage().getParameters();

        assertNotEquals("secret", args[0]);
        assertEquals("secret", SensitiveLogCrypto.decryptWithRawKey(TEST_AES_KEY, String.valueOf(args[0])));
        assertSame(error, args[1]);
        assertSame(error, rewritten.getThrown());
    }

    @Test
    void keepsEventUntouchedWhenNoMarkersPresent() {
        SensitiveLog4jRewritePolicy policy = SensitiveLog4jRewritePolicy.createPolicy();
        LogEvent event = Log4jLogEvent.newBuilder()
            .setLoggerName("pure-log4j2")
            .setLevel(Level.INFO)
            .setMessage(new ParameterizedMessage("Public {}", new Object[] {"alice"}))
            .build();

        LogEvent rewritten = policy.rewrite(event);

        assertSame(event, rewritten);
    }
}

