package io.github.sp4j.sensitivelog.provider;

final class SensitiveLogPropertyResolver {

    static final String AES_KEY_PROPERTY = "sensitivelog.aes-key";

    private SensitiveLogPropertyResolver() {
    }

    static String resolveAesKeyOrThrow() {
        String fromSystem = trimToNull(System.getProperty(AES_KEY_PROPERTY));
        if (fromSystem != null) {
            return fromSystem;
        }

        String fromEnv = trimToNull(System.getenv("SENSITIVELOG_AES_KEY"));
        if (fromEnv != null) {
            return fromEnv;
        }

        throw new IllegalStateException("Missing required property 'sensitivelog.aes-key'. " +
                "Please set it as a JVM property (-Dsensitivelog.aes-key=<key>) or " +
                "environment variable (SENSITIVELOG_AES_KEY=<key>).");
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

