package io.github.sp4j.sensitivelog.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

class SensitiveArgumentSanitizerTest {

    @Test
    void encryptsPlaceholderAfterSensitiveMarker() {
        LoggerFactory.getLogger(SensitiveArgumentSanitizerTest.class);

        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "My password is [SENSITIVE] {}",
            new Object[] {"secret"}
        );

        assertNotEquals("secret", sanitized[0]);
        assertTrue(((String) sanitized[0]).matches("^[A-Za-z0-9+/=]+$"));
    }

    @Test
    void keepsArgumentsWithoutSensitiveMarker() {
        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "My password is {}",
            new Object[] {"secret"}
        );

        assertEquals("secret", sanitized[0]);
    }

    @Test
    void preservesThrowableAsTrailingArgument() {
        LoggerFactory.getLogger(SensitiveArgumentSanitizerTest.class);
        RuntimeException error = new RuntimeException("boom");

        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "Error for [SENSITIVE] {}",
            new Object[] {"secret", error}
        );

        assertNotEquals("secret", sanitized[0]);
        assertSame(error, sanitized[1]);
    }

    @Test
    void encryptsOnlyPlaceholdersAfterMarker() {
        LoggerFactory.getLogger(SensitiveArgumentSanitizerTest.class);
        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "Public {} and private [SENSITIVE] {}",
            new Object[] {"name", "secret"}
        );

        assertEquals("name", sanitized[0]);
        assertNotEquals("secret", sanitized[1]);
    }

    @Test
    void masksFullValueWithMaskedMarker() {
        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "Card [MASKED] {}",
            new Object[] {"123456"}
        );

        assertEquals("******", sanitized[0]);
    }

    @Test
    void masksBothSidesWithMaskedNMarker() {
        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "Card [MASKED_2] {}",
            new Object[] {"12345678"}
        );

        assertEquals("**3456**", sanitized[0]);
    }

    @Test
    void masksOnlyFirstCharactersWithMaskedFirstNMarker() {
        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "Card [MASKED_FIRST_3] {}",
            new Object[] {"12345678"}
        );

        assertEquals("***45678", sanitized[0]);
    }

    @Test
    void masksOnlyLastCharactersWithMaskedLastNMarker() {
        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "Card [MASKED_LAST_3] {}",
            new Object[] {"12345678"}
        );

        assertEquals("12345***", sanitized[0]);
    }

    @Test
    void masksWholeValueWhenMaskedBothRangeCoversAllCharacters() {
        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(
            "Card [MASKED_4] {}",
            new Object[] {"123456"}
        );

        assertEquals("******", sanitized[0]);
    }
}


