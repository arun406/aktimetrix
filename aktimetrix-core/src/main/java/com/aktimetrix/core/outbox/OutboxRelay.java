package com.aktimetrix.core.outbox;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.notification.Notifications;
import com.aktimetrix.core.store.OutboxStore;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.util.Optional;

/**
 * Publishes pending {@link OutboxMessage}s to the message broker, oldest first, and marks them sent.
 * <p>
 * Each message is claimed for {@code aktimetrix.outbox.lease} with an atomic update in the state store, so several
 * application instances can relay side by side. Delivery is at least once: a relay that stops between sending and
 * marking a message lets another send it again after the lease, so consumers should de-duplicate on the event's
 * {@code eventId}. When sending fails, the batch stops and the message is retried once its lease expires.
 * <p>
 * The message key travels in the {@value #MESSAGE_KEY_HEADER} header; each broker module maps it to the broker's own
 * key, such as the Kafka record key or the RabbitMQ routing key.
 */
@Component
public class OutboxRelay {
    private static final Logger logger = LoggerFactory.getLogger(OutboxRelay.class);

    /**
     * Header carrying the message key: the id of the instance the event is about, or the entity id of a dead letter.
     */
    public static final String MESSAGE_KEY_HEADER = "aktimetrixKey";

    private final OutboxStore store;
    private final Sender sender;
    private final AktimetrixProperties properties;
    private final Clock clock;
    private final Notifications notifications;

    @Autowired
    public OutboxRelay(OutboxStore store, StreamBridge streamBridge, AktimetrixProperties properties, Clock clock,
                       Notifications notifications) {
        this(store, streamBridge::send, properties, clock, notifications);
    }

    OutboxRelay(OutboxStore store, Sender sender, AktimetrixProperties properties, Clock clock,
                Notifications notifications) {
        this.store = store;
        this.sender = sender;
        this.properties = properties;
        this.clock = clock;
        this.notifications = notifications;
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
            final Instant now = clock.instant();
            final Optional<OutboxMessage> message = store.claimNext(now, now.plus(properties.getOutbox().getLease()));
            if (message.isEmpty()) {
                break;
            }
            if (Notifications.DESTINATION.equals(message.get().getDestination())) {
                // a notifier that fails holds up nothing else: its notification is retried after its lease
                if (notify(message.get())) {
                    store.markSent(message.get().getId(), clock.instant());
                }
                sent++;
                continue;
            }
            if (!send(message.get())) {
                break;
            }
            store.markSent(message.get().getId(), clock.instant());
            sent++;
        }
        return sent;
    }

    /**
     * Deletes messages sent longer ago than {@code aktimetrix.outbox.retention}.
     */
    @Scheduled(fixedDelayString = "PT1H", initialDelayString = "PT1H")
    public long purge() {
        return store.deleteSentBefore(clock.instant().minus(properties.getOutbox().getRetention()));
    }

    /**
     * @return whether the notification is done with: delivered, or given up after too many attempts
     */
    private boolean notify(OutboxMessage message) {
        if (notifications == null) {
            return true;
        }
        try {
            notifications.deliver(message);
            return true;
        } catch (Exception e) {
            if (message.getAttempts() >= properties.getNotifications().getMaxAttempts()) {
                logger.error("Notification {} given up after {} attempts: {}", message.getId(), message.getAttempts(),
                        e.getMessage());
                return true;
            }
            logger.warn("Notification {} failed (attempt {}); retrying later: {}", message.getId(),
                    message.getAttempts(), e.getMessage());
            return false;
        }
    }

    private boolean send(OutboxMessage message) {
        try {
            final MessageBuilder<byte[]> builder = MessageBuilder
                    .withPayload(message.getPayload().getBytes(StandardCharsets.UTF_8))
                    .setHeader(MessageHeaders.CONTENT_TYPE, MimeTypeUtils.APPLICATION_JSON_VALUE);
            if (message.getMessageKey() != null) {
                builder.setHeader(MESSAGE_KEY_HEADER, message.getMessageKey());
            }
            final boolean sent = sender.send(message.getDestination(), builder.build());
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
