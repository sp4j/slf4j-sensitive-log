package io.github.sp4j.sensitivelog.provider;

import org.apache.logging.log4j.core.LogEvent;
import org.apache.logging.log4j.core.appender.rewrite.RewritePolicy;
import org.apache.logging.log4j.core.config.plugins.Plugin;
import org.apache.logging.log4j.core.config.plugins.PluginFactory;
import org.apache.logging.log4j.core.impl.Log4jLogEvent;
import org.apache.logging.log4j.message.Message;
import org.apache.logging.log4j.message.ParameterizedMessage;

/**
 * Native Log4j2 rewrite policy for projects that do not use SLF4J.
 */
@Plugin(name = "SensitiveLog4jRewritePolicy", category = "Core", elementType = "rewritePolicy", printObject = true)
public final class SensitiveLog4jRewritePolicy implements RewritePolicy {

    private SensitiveLog4jRewritePolicy() {
    }

    @PluginFactory
    public static SensitiveLog4jRewritePolicy createPolicy() {
        return new SensitiveLog4jRewritePolicy();
    }

    @Override
    public LogEvent rewrite(LogEvent source) {
        if (source == null) {
            return null;
        }

        Message message = source.getMessage();
        if (message == null) {
            return source;
        }

        String format = message.getFormat();
        Object[] parameters = message.getParameters();
        if (format == null || parameters == null || parameters.length == 0) {
            return source;
        }

        Object[] sanitized = SensitiveArgumentSanitizer.sanitizeVarargs(format, parameters);
        if (sanitized == parameters) {
            return source;
        }

        ParameterizedMessage rewritten = new ParameterizedMessage(format, sanitized, source.getThrown());
        return new Log4jLogEvent.Builder(source).setMessage(rewritten).build();
    }
}

