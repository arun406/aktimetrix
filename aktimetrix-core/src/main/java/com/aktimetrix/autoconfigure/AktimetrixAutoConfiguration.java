package com.aktimetrix.autoconfigure;

import com.aktimetrix.core.api.EventMapper;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.event.EnvelopeEventMapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.AutoConfigurationPackage;
import org.springframework.boot.autoconfigure.AutoConfigureBefore;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoRepositoriesAutoConfiguration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Wires Aktimetrix into a Spring Boot application: adding the {@code aktimetrix-core} dependency is enough, no
 * {@code @ComponentScan} needed.
 * <p>
 * The framework's package is registered as an auto-configuration package, so Spring Boot creates its MongoDB
 * repositories alongside the application's own.
 */
@Configuration(proxyBeanMethods = false)
@AutoConfigureBefore({MongoDataAutoConfiguration.class, MongoRepositoriesAutoConfiguration.class})
@AutoConfigurationPackage(basePackages = "com.aktimetrix.core")
@ComponentScan("com.aktimetrix.core")
@EnableConfigurationProperties(AktimetrixProperties.class)
@EnableScheduling
public class AktimetrixAutoConfiguration {

    /**
     * Clock of planned, actual and overdue times, in {@code aktimetrix.time-zone}.
     */
    @Bean
    @ConditionalOnMissingBean
    public Clock aktimetrixClock(AktimetrixProperties properties) {
        return Clock.system(properties.getTimeZone());
    }

    /**
     * Reads inbound messages in the Aktimetrix event envelope, unless the application declares its own
     * {@link EventMapper}.
     */
    @Bean
    @ConditionalOnMissingBean(EventMapper.class)
    public EventMapper aktimetrixEventMapper(ObjectMapper objectMapper) {
        return new EnvelopeEventMapper(objectMapper);
    }
}
