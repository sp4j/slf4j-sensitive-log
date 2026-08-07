package io.github.sp4j.sensitivelog.provider;

final class SensitiveArgumentSanitizer {

    private static final String SENSITIVE_MARKER = "[SENSITIVE]";
    private static final String PLACEHOLDER = "{}";

    private SensitiveArgumentSanitizer() {
    }

    static Object sanitizeSingle(String format, Object arg) {
        if (firstSensitivePlaceholderIndex(format) == 0) {
            return SensitiveLogCrypto.encrypt(arg);
        }
        return arg;
    }

    static Object[] sanitizePair(String format, Object arg1, Object arg2) {
        Object[] args = new Object[] {arg1, arg2};
        return sanitizeVarargs(format, args);
    }

    static Object[] sanitizeVarargs(String format, Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return arguments;
        }

        int firstSensitiveArg = firstSensitivePlaceholderIndex(format);
        if (firstSensitiveArg < 0) {
            return arguments;
        }

        int placeholderCount = countPlaceholders(format);
        if (placeholderCount == 0) {
            return arguments;
        }

        int argsLimit = arguments.length;
        if (isTrailingThrowable(arguments, placeholderCount)) {
            argsLimit = arguments.length - 1;
        }

        int upperBound = Math.min(placeholderCount, argsLimit);
        if (firstSensitiveArg >= upperBound) {
            return arguments;
        }

        Object[] sanitized = arguments.clone();
        for (int i = firstSensitiveArg; i < upperBound; i++) {
            sanitized[i] = SensitiveLogCrypto.encrypt(sanitized[i]);
        }
        return sanitized;
    }

    private static int firstSensitivePlaceholderIndex(String format) {
        if (format == null) {
            return -1;
        }

        int markerIndex = format.indexOf(SENSITIVE_MARKER);
        if (markerIndex < 0) {
            return -1;
        }

        int placeholderIndex = 0;
        int searchFrom = 0;
        while (true) {
            int found = format.indexOf(PLACEHOLDER, searchFrom);
            if (found < 0) {
                return -1;
            }
            if (found > markerIndex) {
                return placeholderIndex;
            }
            placeholderIndex++;
            searchFrom = found + PLACEHOLDER.length();
        }
    }

    private static int countPlaceholders(String format) {
        if (format == null) {
            return 0;
        }
        int count = 0;
        int searchFrom = 0;
        while (true) {
            int found = format.indexOf(PLACEHOLDER, searchFrom);
            if (found < 0) {
                return count;
            }
            count++;
            searchFrom = found + PLACEHOLDER.length();
        }
    }

    private static boolean isTrailingThrowable(Object[] arguments, int placeholderCount) {
        return arguments.length > 0
            && arguments[arguments.length - 1] instanceof Throwable
            && placeholderCount < arguments.length;
    }
}

