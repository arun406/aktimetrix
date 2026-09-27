package com.aktimetrix.it;

import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.repository.StepInstanceRepository;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.apache.kafka.common.serialization.StringSerializer;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.test.EmbeddedKafkaBroker;
import org.springframework.kafka.test.context.EmbeddedKafka;
import org.springframework.kafka.test.utils.KafkaTestUtils;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import java.net.InetSocketAddress;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * The smallest possible monitor, {@link ParcelMonitor}: JSON definitions ({@code src/test/resources/aktimetrix}) and
 * one meter, with no {@code @ComponentScan}, process handler or event handler. Runs against an embedded Kafka and an in-memory MongoDB.
 */
@SpringBootTest(classes = ParcelMonitor.class, properties = {
        "aktimetrix.events.topic=parcel-events",
        "aktimetrix.monitor.enabled=false"
})
@EmbeddedKafka(partitions = 1, topics = "parcel-events")
class MinimalMonitorTest {

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

    @Autowired
    private EmbeddedKafkaBroker kafka;
    @Autowired
    private StepInstanceRepository steps;

    @Test
    void definitionsAndOneMeterAreAWorkingMonitor() throws Exception {
        send("{\"tenantKey\":\"T1\",\"eventCode\":\"PARCEL_BOOKED\",\"entityType\":\"parcel\",\"entityId\":\"P-1\","
                + "\"eventUTCTime\":\"2024-01-10 09:00:00\",\"entity\":{\"bookedAt\":\"2024-01-10 09:00:00\"}}");
        StepInstance pickup = await("PICKUP", step -> step.getPlannedAt() != null);
        assertThat(pickup.getPlannedAt()).isEqualTo(LocalDateTime.of(2024, 1, 10, 10, 0));
        assertThat(await("BOOK", step -> "Completed".equals(step.getStatus())).getActualAt())
                .isEqualTo(LocalDateTime.of(2024, 1, 10, 9, 0));

        send("{\"tenantKey\":\"T1\",\"eventCode\":\"PARCEL_PICKED_UP\",\"entityType\":\"parcel\",\"entityId\":\"P-1\","
                + "\"eventUTCTime\":\"2024-01-10 10:20:00\"}");
        pickup = await("PICKUP", step -> "Completed".equals(step.getStatus()));
        assertThat(pickup.getActualAt()).isEqualTo(LocalDateTime.of(2024, 1, 10, 10, 20));
        assertThat(pickup.getTimeliness()).isEqualTo(Timeliness.LATE);
    }

    private void send(String event) {
        Map<String, Object> props = KafkaTestUtils.producerProps(kafka);
        KafkaTemplate<String, String> template = new KafkaTemplate<>(
                new DefaultKafkaProducerFactory<>(props, new StringSerializer(), new StringSerializer()));
        template.send("parcel-events", "P-1", event);
        template.flush();
    }

    private StepInstance await(String stepCode, Predicate<StepInstance> condition)
            throws InterruptedException {
        long deadline = System.currentTimeMillis() + 20_000;
        while (System.currentTimeMillis() < deadline) {
            List<StepInstance> found = steps.findAll();
            for (StepInstance step : found) {
                if (stepCode.equals(step.getStepCode()) && condition.test(step)) {
                    return step;
                }
            }
            Thread.sleep(100);
        }
        throw new AssertionError(stepCode + " did not reach the expected state");
    }
}
