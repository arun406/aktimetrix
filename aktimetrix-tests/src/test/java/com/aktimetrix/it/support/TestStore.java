package com.aktimetrix.it.support;

import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;

import java.util.Map;
import java.util.UUID;

/**
 * A state store started for a test.
 */
public interface TestStore extends AutoCloseable {

    /**
     * Every store module is on the test classpath: keep Spring Boot from connecting to a MongoDB it does not use.
     */
    String WITHOUT_MONGODB = "org.springframework.boot.mongodb.autoconfigure.MongoAutoConfiguration,"
            + "org.springframework.boot.data.mongodb.autoconfigure.DataMongoAutoConfiguration";

    /**
     * Spring properties that select this store and connect an Aktimetrix application to it.
     */
    Map<String, Object> properties();

    @Override
    default void close() {
    }

    static TestStore mongodb() {
        final MongoServer server = new MongoServer(new MemoryBackend());
        final int port = server.bind().getPort();
        return new TestStore() {
            @Override
            public Map<String, Object> properties() {
                return Map.of("aktimetrix.storage.type", "mongodb",
                        "spring.mongodb.uri", "mongodb://localhost:" + port + "/aktimetrix");
            }

            @Override
            public void close() {
                server.shutdown();
            }
        };
    }

    /**
     * H2 in PostgreSQL compatibility mode.
     */
    static TestStore jdbc() {
        final String url = "jdbc:h2:mem:" + UUID.randomUUID() + ";MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1";
        return () -> Map.of("aktimetrix.storage.type", "jdbc", "spring.datasource.url", url,
                "spring.autoconfigure.exclude", WITHOUT_MONGODB);
    }

    static TestStore memory() {
        return () -> Map.of("aktimetrix.storage.type", "memory", "spring.autoconfigure.exclude", WITHOUT_MONGODB);
    }
}
