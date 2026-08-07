package io.github.sp4j.sensitivelog.provider;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import javax.crypto.spec.SecretKeySpec;

public final class SensitiveLogCrypto {

    private static final String AES_TRANSFORMATION = "AES/CBC/PKCS5Padding";
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final byte[] OPENSSL_SALTED_MAGIC = "Salted__".getBytes(StandardCharsets.US_ASCII);
    private static final int PBKDF2_ITERATIONS = 10000;
    private static final int AES_KEY_SIZE_BYTES = 32;
    private static final int PBKDF2_DERIVED_BYTES = 48; // key(32) + iv(16)
    private static final int OPENSSL_SALT_BYTES = 8;
    private static final int CBC_IV_BYTES = 16;
    private static final String REDACTED_VALUE = "[SENSITIVE:REDACTED]";

    private static final Object LOCK = new Object();
    private static final SecureRandom RANDOM = new SecureRandom();

    private static volatile String configuredKey;
    private static volatile String lastInitError;

    private SensitiveLogCrypto() {
    }

    static void initializeFromConfiguration() {
        if (configuredKey != null) {
            return;
        }

        synchronized (LOCK) {
            if (configuredKey != null) {
                return;
            }

            String rawKey = SensitiveLogPropertyResolver.resolveAesKeyOrThrow();
            if (rawKey.trim().isEmpty()) {
                throw new IllegalStateException("Property 'sensitivelog.aes-key' must not be empty");
            }
            configuredKey = rawKey;
        }
    }

    static String encrypt(Object value) {
        if (value == null) {
            return null;
        }

        String key = configuredKey;
        if (key == null) {
            key = tryInitializeFromConfiguration();
        }

        if (key == null) {
            return REDACTED_VALUE;
        }

        return encryptInternal(key, String.valueOf(value));
    }

    public static String encryptWithRawKey(String rawKey, String value) {
        if (value == null) {
            return null;
        }
        return encryptInternal(rawKey, value);
    }

    public static String decryptWithRawKey(String rawKey, String value) {
        if (value == null) {
            return null;
        }
        return decryptInternal(rawKey, value);
    }

    private static String encryptInternal(String rawKey, String value) {
        DerivedMaterial derived = deriveOpenSslMaterial(rawKey);

        byte[] encrypted;
        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, derived.key, derived.ivSpec);
            encrypted = cipher.doFinal(value.getBytes(StandardCharsets.UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to encrypt sensitive value", e);
        }

        byte[] payload = new byte[OPENSSL_SALTED_MAGIC.length + derived.salt.length + encrypted.length];
        System.arraycopy(OPENSSL_SALTED_MAGIC, 0, payload, 0, OPENSSL_SALTED_MAGIC.length);
        System.arraycopy(derived.salt, 0, payload, OPENSSL_SALTED_MAGIC.length, derived.salt.length);
        System.arraycopy(encrypted, 0, payload, OPENSSL_SALTED_MAGIC.length + derived.salt.length, encrypted.length);
        return Base64.getEncoder().encodeToString(payload);
    }

    private static String decryptInternal(String rawKey, String value) {
        byte[] payload;
        try {
            payload = Base64.getDecoder().decode(value);
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Encrypted value is not valid Base64", e);
        }

        if (payload.length <= OPENSSL_SALTED_MAGIC.length + OPENSSL_SALT_BYTES) {
            throw new IllegalStateException("Encrypted value is too short or invalid");
        }

        byte[] marker = Arrays.copyOfRange(payload, 0, OPENSSL_SALTED_MAGIC.length);
        if (!Arrays.equals(marker, OPENSSL_SALTED_MAGIC)) {
            throw new IllegalStateException("Encrypted value is not in OpenSSL salted format");
        }

        byte[] salt = Arrays.copyOfRange(payload, OPENSSL_SALTED_MAGIC.length, OPENSSL_SALTED_MAGIC.length + OPENSSL_SALT_BYTES);
        byte[] encrypted = Arrays.copyOfRange(payload, OPENSSL_SALTED_MAGIC.length + OPENSSL_SALT_BYTES, payload.length);
        DerivedMaterial derived = deriveOpenSslMaterial(rawKey, salt);

        try {
            Cipher cipher = Cipher.getInstance(AES_TRANSFORMATION);
            cipher.init(Cipher.DECRYPT_MODE, derived.key, derived.ivSpec);
            return new String(cipher.doFinal(encrypted), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to decrypt sensitive value", e);
        }
    }

    private static DerivedMaterial deriveOpenSslMaterial(String rawKey) {
        byte[] salt = new byte[OPENSSL_SALT_BYTES];
        RANDOM.nextBytes(salt);
        return deriveOpenSslMaterial(rawKey, salt);
    }

    private static DerivedMaterial deriveOpenSslMaterial(String rawKey, byte[] salt) {
        if (rawKey == null || rawKey.trim().isEmpty()) {
            throw new IllegalStateException("Property 'sensitivelog.aes-key' must not be empty");
        }

        try {
            PBEKeySpec spec = new PBEKeySpec(
                rawKey.toCharArray(),
                salt,
                PBKDF2_ITERATIONS,
                PBKDF2_DERIVED_BYTES * 8
            );
            SecretKeyFactory factory = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
            byte[] material = factory.generateSecret(spec).getEncoded();
            byte[] keyBytes = Arrays.copyOfRange(material, 0, AES_KEY_SIZE_BYTES);
            byte[] ivBytes = Arrays.copyOfRange(material, AES_KEY_SIZE_BYTES, AES_KEY_SIZE_BYTES + CBC_IV_BYTES);
            return new DerivedMaterial(new SecretKeySpec(keyBytes, "AES"), new javax.crypto.spec.IvParameterSpec(ivBytes), salt);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to derive key material", e);
        }
    }

    private static String tryInitializeFromConfiguration() {
        try {
            initializeFromConfiguration();
            return configuredKey;
        } catch (RuntimeException e) {
            lastInitError = e.getMessage();
            return null;
        }
    }

    static String getLastInitError() {
        return lastInitError;
    }

    private static final class DerivedMaterial {
        private final SecretKeySpec key;
        private final javax.crypto.spec.IvParameterSpec ivSpec;
        private final byte[] salt;

        private DerivedMaterial(SecretKeySpec key, javax.crypto.spec.IvParameterSpec ivSpec, byte[] salt) {
            this.key = key;
            this.ivSpec = ivSpec;
            this.salt = salt;
        }
    }
}


