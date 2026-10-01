package com.aktimetrix.it.support;

import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.EmbeddedKafkaKraftBroker;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Predicate;

/**
 * An embedded Kafka broker.
 */
public class KafkaTestBroker implements TestBroker {

    private final EmbeddedKafkaBroker kafka;
    private final KafkaTemplate<String, String> producer;

    public KafkaTestBroker(String eventsTopic) {
        kafka = new EmbeddedKafkaKraftBroker(1, 1, eventsTopic, eventsTopic + ".dlq", "step-instance-out-0",
                "process-instance-out-0", "measurement-instance-out-0");
        kafka.afterPropertiesSet();
        producer = new KafkaTemplate<>(new DefaultKafkaProducerFactory<>(KafkaTestUtils.producerProps(kafka),
                new StringSerializer(), new StringSerializer()));
    }

    @Override
    public Map<String, Object> properties() {
        return Map.of("spring.cloud.stream.defaultBinder", "kafka",
                "spring.cloud.stream.kafka.binder.brokers", kafka.getBrokersAsString());
    }

    @Override
    public void send(String destination, String key, String payload) {
        producer.send(destination, key, payload);
        producer.flush();
    }

    @Override
    public List<String> received(String destination, Predicate<List<String>> enough) {
        final Map<String, Object> props = KafkaTestUtils.consumerProps("verifier-" + UUID.randomUUID(), "false", kafka);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        final List<String> values = new ArrayList<>();
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new StringDeserializer()).createConsumer()) {
            kafka.consumeFromAnEmbeddedTopic(consumer, destination);
            final long deadline = System.currentTimeMillis() + 20_000;
            while (!enough.test(values) && System.currentTimeMillis() < deadline) {
                KafkaTestUtils.getRecords(consumer, Duration.ofMillis(500)).forEach(record -> values.add(record.value()));
            }
        }
        return values;
    }

    @Override
    public void close() {
        producer.destroy();
        kafka.destroy();
    }
}
