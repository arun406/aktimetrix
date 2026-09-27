package com.aktimetrix.it;

import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.outbox.OutboxRepository;
import com.aktimetrix.core.repository.StepInstanceRepository;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BooleanSupplier;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The smallest possible monitor, {@link ParcelMonitor}: JSON definitions ({@code src/test/resources/aktimetrix}) and
 * one meter, with no {@code @ComponentScan}, process handler or event handler. Runs against an embedded Kafka and an
 * in-memory MongoDB.
 * <p>
 * A parcel is booked at 09:00. PICKUP is planned by a meter (+1 h, 10 minutes' tolerance), SORT by a duration from the
 * start (+3 h), and DELIVER 5 h after SORT completes.
 */
@SpringBootTest(classes = {ParcelMonitor.class, MinimalMonitorTest.Metrics.class}, properties = {
        "aktimetrix.events.topic=parcel-events",
        "aktimetrix.monitor.enabled=false"
})
@EmbeddedKafka(partitions = 1, topics = {"parcel-events", "step-instance-out-0", "process-instance-out-0",
        "measurement-instance-out-0"})
class MinimalMonitorTest {

    private static final LocalDateTime BOOKED = LocalDateTime.of(2024, 1, 10, 9, 0);
    private static final MongoServer MONGO = new MongoServer(new MemoryBackend());
    private static final InetSocketAddress MONGO_ADDRESS = MONGO.bind();

    @DynamicPropertySource
    static void infrastructure(DynamicPropertyRegistry registry) {
        registry.add("spring.data.mongodb.uri", () -> "mongodb://localhost:" + MONGO_ADDRESS.getPort() + "/parcels");
        registry.add("spring.cloud.stream.kafka.binder.brokers", () -> "${spring.embedded.kafka.brokers}");
    }

    @AfterAll
    static void stopMongo() {
        MONGO.shutdown();
    }

    @TestConfiguration
    static class Metrics {
        @Bean
        MeterRegistry meterRegistry() {
            return new SimpleMeterRegistry();
        }
    }

    @Autowired
    private EmbeddedKafkaBroker kafka;
    @Autowired
    private StepInstanceRepository steps;
    @Autowired
    private OutboxRepository outbox;
    @Autowired
    private MeterRegistry meterRegistry;
    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void plansByMeterAndByDurationForecastsRiskAndPublishesThroughTheOutbox() throws Exception {
        // booked at 09:00: PICKUP planned 10:00 by the meter, SORT 12:00 by duration, DELIVER not yet
        send("PARCEL_BOOKED", "2024-01-10 09:00:00", "{\"bookedAt\":\"2024-01-10 09:00:00\"}");
        StepInstance pickup = await("PICKUP", step -> step.getLateAfter() != null);
        assertThat(pickup.getPlannedAt()).isEqualTo(BOOKED.plusHours(1));
        assertThat(pickup.getLateAfter()).isEqualTo(BOOKED.plusHours(1).plusMinutes(10));
        StepInstance sort = await("SORT", step -> step.getPlannedAt() != null);
        assertThat(sort.getPlannedAt()).isEqualTo(BOOKED.plusHours(3));
        assertThat(step("DELIVER").getPlannedAt()).isNull();
        assertThat(await("BOOK", step -> "Completed".equals(step.getStatus())).getActualAt()).isEqualTo(BOOKED);

        // picked up at 11:30, 90 minutes late: SORT is forecast for 13:30, past its 12:00 deadline
        send("PARCEL_PICKED_UP", "2024-01-10 11:30:00", null);
        pickup = await("PICKUP", step -> "Completed".equals(step.getStatus()));
        assertThat(pickup.getTimeliness()).isEqualTo(Timeliness.LATE);
        sort = await("SORT", step -> step.getTimeliness() != null);
        assertThat(sort.getTimeliness()).isEqualTo(Timeliness.AT_RISK);
        assertThat(sort.getExpectedAt()).isEqualTo(BOOKED.plusHours(4).plusMinutes(30));

        // sorted at 12:10: late, and DELIVER is planned 5 hours later
        send("PARCEL_SORTED", "2024-01-10 12:10:00", null);
        sort = await("SORT", step -> "Completed".equals(step.getStatus()));
        assertThat(sort.getTimeliness()).isEqualTo(Timeliness.LATE);
        assertThat(await("DELIVER", step -> step.getPlannedAt() != null).getPlannedAt())
                .isEqualTo(LocalDateTime.of(2024, 1, 10, 17, 10));

        // every change reached Kafka through the outbox
        Set<String> stepEvents = stepEventsPublished(Set.of("SORT AT_RISK", "DELIVER PLANNED", "SORT COMPLETED"));
        assertThat(stepEvents).contains("PICKUP CREATED", "SORT AT_RISK", "DELIVER PLANNED", "SORT COMPLETED");
        awaitTrue(() -> outbox.countBySentAtIsNull() == 0);

        assertThat(meterRegistry.get("aktimetrix.processes.started").counter().count()).isEqualTo(1);
        assertThat(meterRegistry.get("aktimetrix.steps.at.risk").counter().count()).isEqualTo(1);
        assertThat(total(meterRegistry.get("aktimetrix.steps.completed").tag("timeliness", "LATE").counters()))
                .as("PICKUP and SORT").isEqualTo(2);
        assertThat(total(meterRegistry.get("aktimetrix.events").tag("outcome", "handled").counters())).isEqualTo(3);
    }

    private static double total(Collection<Counter> counters) {
        return counters.stream().mapToDouble(Counter::count).sum();
    }

    private void send(String eventCode, String utcTime, String entity) {
        String event = "{\"tenantKey\":\"T1\",\"eventCode\":\"" + eventCode + "\",\"entityType\":\"parcel\","
                + "\"entityId\":\"P-1\",\"eventUTCTime\":\"" + utcTime + "\""
                + (entity == null ? "" : ",\"entity\":" + entity) + "}";
        Map<String, Object> props = KafkaTestUtils.producerProps(kafka);
        KafkaTemplate<String, String> template = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));
        template.send("parcel-events", "P-1", event);
        template.flush();
    }

    private StepInstance step(String stepCode) {
        return steps.findAll().stream().filter(step -> stepCode.equals(step.getStepCode())).findFirst().orElse(null);
    }

    private StepInstance await(String stepCode, Predicate<StepInstance> condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            StepInstance step = step(stepCode);
            if (step != null && condition.test(step)) {
                return step;
            }
            Thread.sleep(100);
        }
        throw new AssertionError(stepCode + " did not reach the expected state");
    }

    private static void awaitTrue(BooleanSupplier condition) throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        while (!condition.getAsBoolean()) {
            if (System.currentTimeMillis() > deadline) {
                throw new AssertionError("condition not reached in time");
            }
            Thread.sleep(100);
        }
    }

    /**
     * "STEP EVENTCODE" of every step event published, until the expected ones have arrived.
     */
    private Set<String> stepEventsPublished(Set<String> expected) throws Exception {
        Map<String, Object> props = KafkaTestUtils.consumerProps("verifier", "false", kafka);
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        Set<String> events = new HashSet<>();
        try (Consumer<String, String> consumer = new DefaultKafkaConsumerFactory<>(props, new StringDeserializer(),
                new StringDeserializer()).createConsumer()) {
            kafka.consumeFromAnEmbeddedTopic(consumer, "step-instance-out-0");
            long deadline = System.currentTimeMillis() + 20_000;
            while (!events.containsAll(expected) && System.currentTimeMillis() < deadline) {
                for (ConsumerRecord<String, String> record : KafkaTestUtils.getRecords(consumer, 1000)) {
                    JsonNode event = objectMapper.readTree(record.value());
                    events.add(event.get("entity").get("stepCode").asText() + " " + event.get("eventCode").asText());
                }
            }
        }
        return events;
    }
}
