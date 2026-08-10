package io.github.sp4j.sensitivelog.provider;

final class SensitiveArgumentSanitizer {

    private static final String MASKED_PREFIX = "MASKED_";
    private static final String MASKED_FIRST_PREFIX = "MASKED_FIRST_";
    private static final String MASKED_LAST_PREFIX = "MASKED_LAST_";
    private static final String PLACEHOLDER = "{}";

    private SensitiveArgumentSanitizer() {
    }

    static Object sanitizeSingle(String format, Object arg) {
        Object[] sanitized = sanitizeVarargs(format, new Object[] {arg});
        return sanitized[0];
    }

    static Object[] sanitizePair(String format, Object arg1, Object arg2) {
        Object[] args = new Object[] {arg1, arg2};
        return sanitizeVarargs(format, args);
    }

    static Object[] sanitizeVarargs(String format, Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
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
        if (upperBound == 0) {
            return arguments;
        }

        PlaceholderAction[] actions = resolvePlaceholderActions(format, placeholderCount);
        if (!hasAnyAction(actions, upperBound)) {
            return arguments;
        }

        Object[] sanitized = arguments.clone();
        sanitizeByActionsInPlace(sanitized, actions, upperBound);
        return sanitized;
    }

    static void sanitizeVarargsInPlace(String format, Object[] arguments) {
        if (arguments == null || arguments.length == 0) {
            return;
        }

        int placeholderCount = countPlaceholders(format);
        if (placeholderCount == 0) {
            return;
        }

        int argsLimit = arguments.length;
        if (isTrailingThrowable(arguments, placeholderCount)) {
            argsLimit = arguments.length - 1;
        }

        int upperBound = Math.min(placeholderCount, argsLimit);
        if (upperBound == 0) {
            return;
        }

        PlaceholderAction[] actions = resolvePlaceholderActions(format, placeholderCount);
        if (!hasAnyAction(actions, upperBound)) {
            return;
        }

        sanitizeByActionsInPlace(arguments, actions, upperBound);
    }

    private static PlaceholderAction[] resolvePlaceholderActions(String format, int placeholderCount) {
        PlaceholderAction[] actions = new PlaceholderAction[placeholderCount];
        for (int i = 0; i < actions.length; i++) {
            actions[i] = PlaceholderAction.none();
        }

        if (format == null) {
            return actions;
        }

        PlaceholderAction current = PlaceholderAction.none();
        int placeholderIndex = 0;
        int i = 0;
        while (i < format.length() && placeholderIndex < placeholderCount) {
            if (format.startsWith(PLACEHOLDER, i)) {
                actions[placeholderIndex++] = current;
                i += PLACEHOLDER.length();
                continue;
            }

            if (format.charAt(i) == '[') {
                int markerEnd = format.indexOf(']', i);
                if (markerEnd > i) {
                    PlaceholderAction parsed = parseAction(format.substring(i + 1, markerEnd));
                    if (parsed.isEnabled()) {
                        current = parsed;
                    }
                    i = markerEnd + 1;
                    continue;
                }
            }

            i++;
        }
        return actions;
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

    private static PlaceholderAction parseAction(String rawToken) {
        if ("SENSITIVE".equals(rawToken)) {
            return PlaceholderAction.sensitive();
        }
        if ("MASKED".equals(rawToken)) {
            return PlaceholderAction.maskedAll();
        }
        if (rawToken.startsWith(MASKED_FIRST_PREFIX)) {
            int count = parseNonNegativeInt(rawToken.substring(MASKED_FIRST_PREFIX.length()));
            if (count >= 0) {
                return PlaceholderAction.maskedFirst(count);
            }
            return PlaceholderAction.none();
        }
        if (rawToken.startsWith(MASKED_LAST_PREFIX)) {
            int count = parseNonNegativeInt(rawToken.substring(MASKED_LAST_PREFIX.length()));
            if (count >= 0) {
                return PlaceholderAction.maskedLast(count);
            }
            return PlaceholderAction.none();
        }
        if (rawToken.startsWith(MASKED_PREFIX)) {
            int count = parseNonNegativeInt(rawToken.substring(MASKED_PREFIX.length()));
            if (count >= 0) {
                return PlaceholderAction.maskedBoth(count);
            }
        }
        return PlaceholderAction.none();
    }

    private static int parseNonNegativeInt(String value) {
        if (value == null || value.isEmpty()) {
            return -1;
        }
        for (int i = 0; i < value.length(); i++) {
            if (!Character.isDigit(value.charAt(i))) {
                return -1;
            }
        }
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException ignored) {
            return -1;
        }
    }

    private static boolean hasAnyAction(PlaceholderAction[] actions, int upperBound) {
        for (int i = 0; i < upperBound; i++) {
            if (actions[i].isEnabled()) {
                return true;
            }
        }
        return false;
    }

    private static void sanitizeByActionsInPlace(Object[] arguments, PlaceholderAction[] actions, int endExclusive) {
        for (int i = 0; i < endExclusive; i++) {
            arguments[i] = actions[i].apply(arguments[i]);
        }
    }

    private static String repeatStars(int count) {
        if (count <= 0) {
            return "";
        }
        char[] stars = new char[count];
        for (int i = 0; i < count; i++) {
            stars[i] = '*';
        }
        return new String(stars);
    }

    private static String maskAll(String value) {
        return repeatStars(value.length());
    }

    private static String maskFirst(String value, int count) {
        if (count <= 0) {
            return value;
        }
        int applied = Math.min(count, value.length());
        return repeatStars(applied) + value.substring(applied);
    }

    private static String maskLast(String value, int count) {
        if (count <= 0) {
            return value;
        }
        int applied = Math.min(count, value.length());
        return value.substring(0, value.length() - applied) + repeatStars(applied);
    }

    private static String maskBoth(String value, int count) {
        if (count <= 0) {
            return value;
        }
        if (value.length() <= count * 2) {
            return maskAll(value);
        }
        return repeatStars(count) + value.substring(count, value.length() - count) + repeatStars(count);
    }

    private static final class PlaceholderAction {
        private static final int NONE = 0;
        private static final int SENSITIVE = 1;
        private static final int MASKED_ALL = 2;
        private static final int MASKED_BOTH = 3;
        private static final int MASKED_FIRST = 4;
        private static final int MASKED_LAST = 5;

        private final int kind;
        private final int count;

        private PlaceholderAction(int kind, int count) {
            this.kind = kind;
            this.count = count;
        }

        static PlaceholderAction none() {
            return new PlaceholderAction(NONE, 0);
        }

        static PlaceholderAction sensitive() {
            return new PlaceholderAction(SENSITIVE, 0);
        }

        static PlaceholderAction maskedAll() {
            return new PlaceholderAction(MASKED_ALL, 0);
        }

        static PlaceholderAction maskedBoth(int count) {
            return new PlaceholderAction(MASKED_BOTH, count);
        }

        static PlaceholderAction maskedFirst(int count) {
            return new PlaceholderAction(MASKED_FIRST, count);
        }

        static PlaceholderAction maskedLast(int count) {
            return new PlaceholderAction(MASKED_LAST, count);
        }

        boolean isEnabled() {
            return kind != NONE;
        }

        Object apply(Object value) {
            if (kind == NONE || value == null) {
                return value;
            }

            if (kind == SENSITIVE) {
                return SensitiveLogCrypto.encrypt(value);
            }

            String asString = String.valueOf(value);
            if (kind == MASKED_ALL) {
                return maskAll(asString);
            }
            if (kind == MASKED_BOTH) {
                return maskBoth(asString, count);
            }
            if (kind == MASKED_FIRST) {
                return maskFirst(asString, count);
            }
            if (kind == MASKED_LAST) {
                return maskLast(asString, count);
            }
            return value;
        }
    }
}

