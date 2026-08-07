package io.github.sp4j.sensitivelog.tool;

import io.github.sp4j.sensitivelog.provider.SensitiveLogCrypto;

/**
 * CLI entry point for encryption.
 * Args: <aes-key> <plain-text>
 */
public final class SensitiveLogEncrypt {

    private SensitiveLogEncrypt() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -cp <classpath> io.github.sp4j.sensitivelog.tool.SensitiveLogEncrypt <aes-key> <plain-text>");
            System.exit(1);
        }

        String key = args[0];
        String plainText = args[1];
        String encrypted = SensitiveLogCrypto.encryptWithRawKey(key, plainText);
        System.out.println(encrypted);
    }
}

