package com.aktimetrix.autoconfigure;

import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.core.env.MapPropertySource;
import org.springframework.core.env.StandardEnvironment;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class AktimetrixDefaultPropertiesTest {

    @Test
    void defaultsBindTheInboundTopicAndYieldToTheApplication() {
        StandardEnvironment environment = new StandardEnvironment();
        environment.getPropertySources().addFirst(new MapPropertySource("application",
                Map.of("aktimetrix.events.topic", "order-events",
                        "spring.cloud.stream.bindings.processor-in-0.group", "monitors")));

        new AktimetrixDefaultProperties().postProcessEnvironment(environment, new SpringApplication());

        assertThat(environment.getProperty("spring.cloud.stream.function.definition")).isEqualTo("processor");
        assertThat(environment.getProperty("spring.cloud.stream.bindings.processor-in-0.destination"))
                .isEqualTo("order-events");
        assertThat(environment.getProperty("spring.cloud.stream.bindings.processor-in-0.group")).isEqualTo("monitors");
    }
}
