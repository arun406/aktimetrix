package com.aktimetrix.autoconfigure;

import com.aktimetrix.core.api.EventMapper;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.event.EnvelopeEventMapper;
import tools.jackson.databind.ObjectMapper;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.time.Clock;

/**
 * Wires Aktimetrix into a Spring Boot application: adding the {@code aktimetrix-core} dependency, one store module and
 * one broker module is enough, no {@code @ComponentScan} needed.
 * <p>
 * The store module supplies the {@link com.aktimetrix.core.store state-store contract}; the broker module, the
 * Spring Cloud Stream binder and its defaults.
 */
@Configuration(proxyBeanMethods = false)
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
