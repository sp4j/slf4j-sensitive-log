package io.github.sp4j.sensitivelog.provider;

import java.util.ServiceLoader;
import org.slf4j.ILoggerFactory;
import org.slf4j.IMarkerFactory;
import org.slf4j.helpers.NOP_FallbackServiceProvider;
import org.slf4j.spi.MDCAdapter;
import org.slf4j.spi.SLF4JServiceProvider;

public final class SensitiveLogServiceProvider implements SLF4JServiceProvider {

    private static final String REQUESTED_API_VERSION = "2.0.99";

    private volatile ILoggerFactory loggerFactory;
    private volatile IMarkerFactory markerFactory;
    private volatile MDCAdapter mdcAdapter;

    @Override
    public void initialize() {
        SensitiveLogCrypto.initializeFromConfiguration();
        SLF4JServiceProvider delegateProvider = findDelegateProvider();
        delegateProvider.initialize();
        this.loggerFactory = new SensitiveLoggerFactory(delegateProvider.getLoggerFactory());
        this.markerFactory = delegateProvider.getMarkerFactory();
        this.mdcAdapter = delegateProvider.getMDCAdapter();
    }

    @Override
    public ILoggerFactory getLoggerFactory() {
        return loggerFactory;
    }

    @Override
    public IMarkerFactory getMarkerFactory() {
        return markerFactory;
    }

    @Override
    public MDCAdapter getMDCAdapter() {
        return mdcAdapter;
    }

    @Override
    public String getRequestedApiVersion() {
        return REQUESTED_API_VERSION;
    }

    private SLF4JServiceProvider findDelegateProvider() {
        ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
        for (SLF4JServiceProvider candidate : ServiceLoader.load(SLF4JServiceProvider.class, classLoader)) {
            if (!candidate.getClass().equals(SensitiveLogServiceProvider.class)) {
                return candidate;
            }
        }
        return new NOP_FallbackServiceProvider();
    }
}

