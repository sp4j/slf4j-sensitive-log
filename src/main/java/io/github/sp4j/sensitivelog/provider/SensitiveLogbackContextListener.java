package io.github.sp4j.sensitivelog.provider;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.classic.spi.LoggerContextListener;

final class SensitiveLogbackContextListener implements LoggerContextListener {

    @Override
    public boolean isResetResistant() {
        return true;
    }

    @Override
    public void onStart(LoggerContext context) {
        SensitiveLogbackConfigurer.installIfLogbackPresent(context);
    }

    @Override
    public void onReset(final LoggerContext context) {
        SensitiveLogbackConfigurer.installIfLogbackPresent(context);
        Thread reinstallThread = new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(50L);
                } catch (InterruptedException ex) {
                    Thread.currentThread().interrupt();
                }
                SensitiveLogbackConfigurer.installIfLogbackPresent(context);
            }
        }, "sensitive-logback-reinstall");
        reinstallThread.setDaemon(true);
        reinstallThread.start();
    }

    @Override
    public void onStop(LoggerContext context) {
    }

    @Override
    public void onLevelChange(Logger logger, Level level) {
    }
}

