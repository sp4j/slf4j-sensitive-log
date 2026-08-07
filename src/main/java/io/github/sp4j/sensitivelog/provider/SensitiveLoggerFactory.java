package io.github.sp4j.sensitivelog.provider;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.ILoggerFactory;
import org.slf4j.Logger;

public final class SensitiveLoggerFactory implements ILoggerFactory {

    private final ILoggerFactory delegateFactory;
    private final Map<String, Logger> cache = new ConcurrentHashMap<>();

    public SensitiveLoggerFactory(ILoggerFactory delegateFactory) {
        this.delegateFactory = delegateFactory;
    }

    @Override
    public Logger getLogger(String name) {
        return cache.computeIfAbsent(name, key -> new SensitiveLogger(delegateFactory.getLogger(key)));
    }
}

