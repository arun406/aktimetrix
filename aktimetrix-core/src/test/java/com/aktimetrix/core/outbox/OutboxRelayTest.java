package com.aktimetrix.core.outbox;

import java.util.UUID;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import com.aktimetrix.core.store.OutboxStore;
import org.springframework.messaging.Message;

import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2024-01-10T09:00:00Z");

    @Mock
    private OutboxStore store;
    private final List<Message<?>> sent = new ArrayList<>();

    private OutboxRelay relay(OutboxRelay.Sender sender) {
        return new OutboxRelay(store, sender, new AktimetrixProperties(),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void sendsPendingMessagesInOrderAndMarksThemSent() {
        OutboxMessage first = message("{\"n\":1}");
        OutboxMessage second = message("{\"n\":2}");
        when(store.claimNext(NOW, NOW.plusSeconds(30))).thenReturn(Optional.of(first), Optional.of(second), Optional.empty());

        assertThat(relay((destination, message) -> sent.add(message)).relay()).isEqualTo(2);

        assertThat(sent).hasSize(2);
        assertThat(new String((byte[]) sent.get(0).getPayload(), StandardCharsets.UTF_8)).isEqualTo("{\"n\":1}");
        assertThat(new String((byte[]) sent.get(1).getPayload(), StandardCharsets.UTF_8)).isEqualTo("{\"n\":2}");
        assertThat(sent.get(0).getHeaders().get(OutboxRelay.MESSAGE_KEY_HEADER)).isEqualTo("key-1");
        verify(store).markSent(first.getId(), NOW);
        verify(store).markSent(second.getId(), NOW);
    }

    @Test
    void stopsAndLeavesTheMessagePendingWhenSendingFails() {
        when(store.claimNext(any(), any())).thenReturn(Optional.of(message("{}")));

        assertThat(relay((destination, message) -> {
            throw new IllegalStateException("broker down");
        }).relay()).isZero();

        verify(store, never()).markSent(any(), any());
    }

    private static OutboxMessage message(String payload) {
        OutboxMessage message = new OutboxMessage("step-instance-out-0", "key-1", payload, NOW);
        message.setId(UUID.randomUUID().toString());
        return message;
    }
}
