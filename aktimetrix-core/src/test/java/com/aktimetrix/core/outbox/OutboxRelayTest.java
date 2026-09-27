package com.aktimetrix.core.outbox;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2024-01-10T09:00:00Z");

    @Mock
    private MongoTemplate mongoTemplate;
    @Mock
    private OutboxRepository repository;
    private final List<Message<?>> sent = new ArrayList<>();

    private OutboxRelay relay(OutboxRelay.Sender sender) {
        return new OutboxRelay(mongoTemplate, repository, sender, new AktimetrixProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void sendsPendingMessagesInOrderAndMarksThemSent() {
        OutboxMessage first = message("{\"n\":1}");
        OutboxMessage second = message("{\"n\":2}");
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(OutboxMessage.class))).thenReturn(first, second, null);

        assertThat(relay((destination, message) -> sent.add(message)).relay()).isEqualTo(2);

        assertThat(sent).hasSize(2);
        assertThat(new String((byte[]) sent.get(0).getPayload(), StandardCharsets.UTF_8)).isEqualTo("{\"n\":1}");
        assertThat(new String((byte[]) sent.get(1).getPayload(), StandardCharsets.UTF_8)).isEqualTo("{\"n\":2}");
        assertThat(sent.get(0).getHeaders().get(KafkaHeaders.MESSAGE_KEY)).isEqualTo("key-1");
        verify(mongoTemplate, times(2)).updateFirst(any(Query.class), any(Update.class), eq(OutboxMessage.class));
    }

    @Test
    void stopsAndLeavesTheMessagePendingWhenSendingFails() {
        when(mongoTemplate.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
                eq(OutboxMessage.class))).thenReturn(message("{}"));

        assertThat(relay((destination, message) -> {
            throw new IllegalStateException("broker down");
        }).relay()).isZero();

        verify(mongoTemplate, never()).updateFirst(any(Query.class), any(Update.class), eq(OutboxMessage.class));
    }

    private static OutboxMessage message(String payload) {
        OutboxMessage message = new OutboxMessage("step-instance-out-0", "key-1", payload, NOW);
        message.setId(new ObjectId());
        return message;
    }
}
