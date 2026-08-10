package io.github.sp4j.sensitivelog.spring;

import io.github.sp4j.sensitivelog.provider.SensitiveLogbackConfigurer;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;

@AutoConfiguration
@ConditionalOnClass(name = "ch.qos.logback.classic.LoggerContext")
public class SensitiveLogAutoConfiguration {

    @org.springframework.context.annotation.Bean
    static BeanFactoryPostProcessor sensitiveLogBeanFactoryPostProcessor() {
        return new BeanFactoryPostProcessor() {
            @Override
            public void postProcessBeanFactory(ConfigurableListableBeanFactory beanFactory) throws BeansException {
                SensitiveLogbackConfigurer.installIfLogbackPresent();
            }
        };
    }
}

