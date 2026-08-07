package io.github.sp4j.sensitivelog.provider;

import java.io.IOException;
import java.io.InputStream;
import java.util.Enumeration;
import java.util.Properties;

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

        String fromProperties = loadFromProperties();
        if (fromProperties != null) {
            return fromProperties;
        }

        throw new IllegalStateException("Missing required property 'sensitivelog.aes-key'");
    }

    private static String loadFromProperties() {
        try {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            Enumeration<InputStream> streams = openAll(classLoader, "application.properties");
            while (streams.hasMoreElements()) {
                InputStream inputStream = streams.nextElement();
                try {
                    Properties properties = new Properties();
                    properties.load(inputStream);
                    String value = trimToNull(properties.getProperty(AES_KEY_PROPERTY));
                    if (value != null) {
                        return value;
                    }
                } finally {
                    inputStream.close();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to read application.properties", e);
        }
        return null;
    }

    private static Enumeration<InputStream> openAll(ClassLoader classLoader, String name) throws IOException {
        final Enumeration<java.net.URL> urls = classLoader.getResources(name);
        return new Enumeration<InputStream>() {
            @Override
            public boolean hasMoreElements() {
                return urls.hasMoreElements();
            }

            @Override
            public InputStream nextElement() {
                try {
                    return urls.nextElement().openStream();
                } catch (IOException e) {
                    throw new IllegalStateException("Failed to open resource: " + name, e);
                }
            }
        };
    }

    private static String trimToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

