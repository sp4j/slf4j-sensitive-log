package io.github.sp4j.sensitivelog.provider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.turbo.TurboFilter;
import ch.qos.logback.core.spi.FilterReply;
import org.slf4j.Marker;

final class SensitiveLogbackTurboFilter extends TurboFilter {

    @Override
    public FilterReply decide(Marker marker, Logger logger, Level level, String format, Object[] params, Throwable t) {
        SensitiveArgumentSanitizer.sanitizeVarargsInPlace(format, params);
        return FilterReply.NEUTRAL;
    }
}

