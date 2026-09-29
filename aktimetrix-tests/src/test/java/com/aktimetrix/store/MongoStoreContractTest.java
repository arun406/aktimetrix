package com.aktimetrix.store;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.store.mongodb.MongoStoreAutoConfiguration;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.store.mongodb.MongoStoreInitializer;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.autoconfigure.ImportAutoConfiguration;
import org.springframework.boot.autoconfigure.data.mongo.MongoDataAutoConfiguration;
import org.springframework.boot.autoconfigure.mongo.MongoAutoConfiguration;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Configuration;

import static org.assertj.core.api.Assertions.assertThat;

import java.net.InetSocketAddress;
import java.util.List;

/**
 * The MongoDB store on an in-memory MongoDB server, which has no transactions.
 */
class MongoStoreContractTest extends StoreContractTest {

    private final MongoServer server = new MongoServer(new MemoryBackend());

    @Configuration(proxyBeanMethods = false)
    @EnableConfigurationProperties(AktimetrixProperties.class)
    @ImportAutoConfiguration({MongoAutoConfiguration.class, MongoStoreAutoConfiguration.class,
            MongoDataAutoConfiguration.class})
    static class Store {
    }

    @Override
    protected ConfigurableApplicationContext start() {
        final InetSocketAddress address = server.bind();
        return new SpringApplicationBuilder(Store.class).web(WebApplicationType.NONE)
                .properties("aktimetrix.storage.type=mongodb",
                        "spring.data.mongodb.uri=mongodb://localhost:" + address.getPort() + "/contract")
                .run();
    }

    /**
     * Instances saved by earlier versions referenced their process and step instance by object id, and had no
     * revision: they are upgraded at startup, and read and updated as usual afterwards.
     */
    @Test
    void upgradesInstancesSavedByEarlierVersions() {
        final MongoTemplate mongo = context().getBean(MongoTemplate.class);
        final ObjectId processId = new ObjectId();
        final ObjectId stepId = new ObjectId();
        mongo.getCollection("processInstances").insertOne(new Document("_id", processId).append("tenant", "OLD")
                .append("processCode", "ORDER_DELIVERY").append("entityType", "com.ecom.order").append("entityId", "9")
                .append("status", "Created").append("complete", false));
        mongo.getCollection("stepInstances").insertOne(new Document("_id", stepId).append("tenant", "OLD")
                .append("processInstanceId", processId).append("stepCode", "SHIP").append("sequence", 0)
                .append("status", "Created"));
        mongo.getCollection("measurement-instance").insertOne(new Document("tenant", "OLD")
                .append("processInstanceId", processId).append("stepInstanceId", stepId).append("code", "TIME")
                .append("type", "P").append("value", "2024-03-01T10:00"));

        context().getBean(MongoStoreInitializer.class).upgrade();

        final ProcessInstance process = processes.findById("OLD", processId.toHexString()).orElseThrow();
        assertThat(process.getRevision()).isZero();
        process.setStatus("Completed");
        processes.save(process);
        final List<StepInstance> found = steps.findByProcessInstance("OLD", processId.toHexString());
        assertThat(found).singleElement().satisfies(step -> {
            assertThat(step.getId()).isEqualTo(stepId.toHexString());
            assertThat(step.getRevision()).isZero();
        });
        assertThat(measurements.find("OLD", processId.toHexString(), stepId.toHexString(), "TIME", "P")).hasSize(1);
    }

    @AfterAll
    void stopServer() {
        server.shutdown();
    }
}
