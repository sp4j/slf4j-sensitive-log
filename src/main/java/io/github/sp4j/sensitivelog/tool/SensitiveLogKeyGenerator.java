package io.github.sp4j.sensitivelog.tool;

import java.security.SecureRandom;
import java.util.Base64;

/**
 * CLI utility to generate AES-256 keys for sensitivelog.aes-key.
 */
public final class SensitiveLogKeyGenerator {

    private static final int AES_256_KEY_SIZE_BYTES = 32;
    private static final String DEFAULT_PROPERTY_NAME = "sensitivelog.aes-key";

    private SensitiveLogKeyGenerator() {
    }

    public static void main(String[] args) {
        if (containsHelp(args)) {
            System.out.println(usage());
            return;
        }

        try {
            Options options = Options.parse(args);
            byte[] key = new byte[AES_256_KEY_SIZE_BYTES];
            new SecureRandom().nextBytes(key);
            String encoded = encode(key, options.format);

            if (options.valueOnly) {
                System.out.println(encoded);
            } else {
                System.out.println(options.propertyName + "=" + encoded);
            }
        } catch (IllegalArgumentException e) {
            System.err.println(e.getMessage());
            System.err.println();
            System.err.println(usage());
            System.exit(1);
        }
    }

    private static boolean containsHelp(String[] args) {
        for (String arg : args) {
            if ("--help".equals(arg) || "-h".equals(arg)) {
                return true;
            }
        }
        return false;
    }

    private static String encode(byte[] key, Format format) {
        if (format == Format.BASE64) {
            return Base64.getEncoder().encodeToString(key);
        }
        if (format == Format.BASE64_URL) {
            return Base64.getUrlEncoder().withoutPadding().encodeToString(key);
        }
        return toHex(key);
    }

    private static String toHex(byte[] bytes) {
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            sb.append(String.format("%02x", b & 0xff));
        }
        return sb.toString();
    }

    private static String usage() {
        return "Usage:\n"
            + "  java -cp <classpath> io.github.sp4j.sensitivelog.tool.SensitiveLogKeyGenerator [options]\n\n"
            + "Options:\n"
            + "  --format=<base64|base64url|hex>   Output format (default: base64)\n"
            + "  --property-name=<name>            Property name in output (default: sensitivelog.aes-key)\n"
            + "  --value-only                      Print only key value\n"
            + "  --help                            Show this help\n";
    }

    private enum Format {
        BASE64,
        BASE64_URL,
        HEX;

        static Format fromUserValue(String value) {
            if ("base64".equalsIgnoreCase(value)) {
                return BASE64;
            }
            if ("base64url".equalsIgnoreCase(value)) {
                return BASE64_URL;
            }
            if ("hex".equalsIgnoreCase(value)) {
                return HEX;
            }
            throw new IllegalArgumentException("Unknown format: " + value);
        }
    }

    private static final class Options {
        private final Format format;
        private final String propertyName;
        private final boolean valueOnly;

        private Options(Format format, String propertyName, boolean valueOnly) {
            this.format = format;
            this.propertyName = propertyName;
            this.valueOnly = valueOnly;
        }

        static Options parse(String[] args) {
            Format format = Format.BASE64;
            String propertyName = DEFAULT_PROPERTY_NAME;
            boolean valueOnly = false;

            for (String arg : args) {
                if (arg.startsWith("--format=")) {
                    format = Format.fromUserValue(valueAfterEquals(arg));
                    continue;
                }

                if (arg.startsWith("--property-name=")) {
                    propertyName = valueAfterEquals(arg);
                    if (propertyName.trim().isEmpty()) {
                        throw new IllegalArgumentException("--property-name cannot be empty");
                    }
                    continue;
                }

                if ("--value-only".equals(arg)) {
                    valueOnly = true;
                    continue;
                }

                throw new IllegalArgumentException("Unknown argument: " + arg);
            }

            return new Options(format, propertyName, valueOnly);
        }

        private static String valueAfterEquals(String arg) {
            int index = arg.indexOf('=');
            if (index < 0 || index == arg.length() - 1) {
                throw new IllegalArgumentException("Expected value in argument: " + arg);
            }
            return arg.substring(index + 1);
        }
    }
}


