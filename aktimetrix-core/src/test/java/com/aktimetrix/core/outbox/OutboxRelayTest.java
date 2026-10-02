package com.aktimetrix.core.outbox;

import java.util.UUID;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.notification.Notifications;
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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OutboxRelayTest {

    private static final Instant NOW = Instant.parse("2024-01-10T09:00:00Z");

    @Mock
    private OutboxStore store;
    private final List<Message<?>> sent = new ArrayList<>();

    @Mock
    private Notifications notifications;

    private OutboxRelay relay(OutboxRelay.Sender sender) {
        return new OutboxRelay(store, sender, new AktimetrixProperties(), Clock.fixed(NOW, ZoneOffset.UTC),
                notifications);
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

    @Test
    void handsNotificationsToTheNotifiersAndKeepsSendingWhenOneFails() throws Exception {
        OutboxMessage failing = notification(1);
        OutboxMessage event = message("{\"n\":1}");
        when(store.claimNext(any(), any())).thenReturn(Optional.of(failing), Optional.of(event), Optional.empty());
        doThrow(new IllegalStateException("webhook down")).when(notifications).deliver(failing);

        assertThat(relay((destination, message) -> sent.add(message)).relay()).isEqualTo(2);

        verify(store, never()).markSent(eq(failing.getId()), any());
        assertThat(sent).as("the event was still published").hasSize(1);
        verify(store).markSent(event.getId(), NOW);
    }

    @Test
    void aNotificationIsMarkedDoneWhenDeliveredOrGivenUp() throws Exception {
        OutboxMessage delivered = notification(1);
        OutboxMessage exhausted = notification(new AktimetrixProperties().getNotifications().getMaxAttempts());
        when(store.claimNext(any(), any())).thenReturn(Optional.of(delivered), Optional.of(exhausted), Optional.empty());
        // lenient: the other notification is delivered with arguments this stub does not match
        lenient().doThrow(new IllegalStateException("webhook down")).when(notifications).deliver(exhausted);

        assertThat(relay((destination, message) -> sent.add(message)).relay()).isEqualTo(2);

        verify(notifications).deliver(delivered);
        verify(store).markSent(delivered.getId(), NOW);
        verify(store).markSent(exhausted.getId(), NOW);
        assertThat(sent).as("notifications never reach the broker").isEmpty();
    }

    private static OutboxMessage notification(int attempts) {
        OutboxMessage message = new OutboxMessage(Notifications.DESTINATION, "key-1",
                "{\"id\":\"" + UUID.randomUUID() + "\"}", NOW);
        message.setId(UUID.randomUUID().toString());
        message.setAttempts(attempts);
        return message;
    }

    private static OutboxMessage message(String payload) {
        OutboxMessage message = new OutboxMessage("step-instance-out-0", "key-1", payload, NOW);
        message.setId(UUID.randomUUID().toString());
        return message;
    }
}
