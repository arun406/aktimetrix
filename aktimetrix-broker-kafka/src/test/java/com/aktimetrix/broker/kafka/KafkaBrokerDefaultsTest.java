package com.aktimetrix.broker.kafka;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class KafkaBrokerDefaultsTest {

    @Test
    void deadLettersFailedEventsAndKeysPublishedOnesYieldingToTheApplication() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("application", Map.of(
                "aktimetrix.events.topic", "order-events",
                "spring.cloud.stream.kafka.bindings.processor-in-0.consumer.enableDlq", "false")));

        new KafkaBrokerDefaults().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.stream.kafka.bindings.processor-in-0.consumer.dlqName"))
                .isEqualTo("order-events.dlq");
        assertThat(environment.getProperty("spring.cloud.stream.kafka.bindings.processor-in-0.consumer.enableDlq"))
                .isEqualTo("false");
        assertThat(environment.getProperty("spring.cloud.stream.kafka.bindings.step-instance-out-0.producer.messageKeyExpression"))
                .isEqualTo("headers['aktimetrixKey']");
    }
}
