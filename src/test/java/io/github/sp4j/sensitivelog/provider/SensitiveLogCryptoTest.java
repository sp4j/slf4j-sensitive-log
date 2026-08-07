package io.github.sp4j.sensitivelog.provider;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import org.junit.jupiter.api.Test;

class SensitiveLogCryptoTest {

    @Test
    void encryptAndDecryptRoundTripWithRawKey() {
        String key = "0123456789abcdef0123456789abcdef";
        String plainText = "secret";

        String encrypted = SensitiveLogCrypto.encryptWithRawKey(key, plainText);

        assertNotEquals(plainText, encrypted);
        assertEquals(plainText, SensitiveLogCrypto.decryptWithRawKey(key, encrypted));
    }
}

