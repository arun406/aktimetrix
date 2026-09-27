package com.aktimetrix.core.outbox;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;

/**
 * Publishes pending {@link OutboxMessage}s to Kafka, oldest first, and marks them sent.
 * <p>
 * Each message is claimed for {@code aktimetrix.outbox.lease} with an atomic update, so several application instances
 * can relay side by side. Delivery is at least once: a relay that stops between sending and marking a message lets
 * another send it again after the lease, so consumers should de-duplicate on the event's {@code eventId}. When
 * sending fails, the batch stops and the message is retried once its lease expires.
 */
@Component
public class OutboxRelay {
    private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);

    private final MongoTemplate mongoTemplate;
    private final OutboxRepository repository;
    private final Sender sender;
    private final AktimetrixProperties properties;
    private final Clock clock;

    @Autowired
    public OutboxRelay(MongoTemplate mongoTemplate, OutboxRepository repository, StreamBridge streamBridge,
                       AktimetrixProperties properties, Clock clock) {
        this(mongoTemplate, repository, streamBridge::send, properties, clock);
    }

    OutboxRelay(MongoTemplate mongoTemplate, OutboxRepository repository, Sender sender,
                AktimetrixProperties properties, Clock clock) {
        this.mongoTemplate = mongoTemplate;
        this.repository = repository;
        this.sender = sender;
        this.properties = properties;
        this.clock = clock;
    }

    /**
     * Sends a message to a Spring Cloud Stream binding.
     */
    interface Sender {
        boolean send(String destination, Message<?> message);
    }

    /**
     * @return how many messages were sent
     */
    @Scheduled(fixedDelayString = "${aktimetrix.outbox.relay-interval:PT1S}")
    public int relay() {
        int sent = 0;
        while (sent < properties.getOutbox().getBatchSize()) {
            final OutboxMessage message = claimNext();
            if (message == null) {
                break;
            }
            if (!send(message)) {
                break;
            }
            mongoTemplate.updateFirst(Query.query(Criteria.where("_id").is(message.getId())),
                    new Update().set("sentAt", clock.instant()).unset("lockedUntil"), OutboxMessage.class);
            sent++;
        }
        return sent;
    }

    /**
     * Deletes messages sent longer ago than {@code aktimetrix.outbox.retention}.
     */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    public long purge() {
        return repository.deleteBySentAtBefore(clock.instant().minus(properties.getOutbox().getRetention()));
    }

    private OutboxMessage claimNext() {
        final Instant now = clock.instant();
        final Query pending = Query.query(Criteria.where("sentAt").is(null).orOperator(
                        Criteria.where("lockedUntil").is(null), Criteria.where("lockedUntil").lt(now)))
                .with(Sort.by("createdAt", "_id"));
        final Update claim = new Update().set("lockedUntil", now.plus(properties.getOutbox().getLease())).inc("attempts", 1);
        return mongoTemplate.findAndModify(pending, claim, FindAndModifyOptions.options().returnNew(true),
                OutboxMessage.class);
    }

    private boolean send(OutboxMessage message) {
        try {
            final boolean sent = sender.send(message.getDestination(), MessageBuilder
                    .withPayload(message.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setHeader(KafkaHeaders.MESSAGE_KEY, message.getMessageKey())
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE)
                    .build());
            if (!sent) {
                logger.warn("Outbox message {} to {} was not sent; retrying later", message.getId(), message.getDestination());
            }
            return sent;
        } catch (RuntimeException e) {
            logger.warn("Outbox message {} to {} failed (attempt {}); retrying later: {}", message.getId(),
                    message.getDestination(), message.getAttempts(), e.getMessage());
            return false;
        }
    }
}
