package io.github.sp4j.sensitivelog.tool;

import io.github.sp4j.sensitivelog.provider.SensitiveLogCrypto;

/**
 * CLI entry point for decryption.
 * Args: <aes-key> <encrypted-base64>
 */
public final class SensitiveLogDecrypt {

    private SensitiveLogDecrypt() {
    }

    public static void main(String[] args) {
        if (args.length != 2) {
            System.err.println("Usage: java -cp <classpath> io.github.sp4j.sensitivelog.tool.SensitiveLogDecrypt <aes-key> <encrypted-base64url>");
            System.exit(1);
        }

        String key = args[0];
        String encryptedValue = args[1];
        String decrypted = SensitiveLogCrypto.decryptWithRawKey(key, encryptedValue);
        System.out.println(decrypted);
    }
}

