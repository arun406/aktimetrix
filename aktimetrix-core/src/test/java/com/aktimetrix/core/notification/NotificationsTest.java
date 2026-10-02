package com.aktimetrix.core.notification;

import com.aktimetrix.core.api.Notification;
import com.aktimetrix.core.api.Notifier;
import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class NotificationsTest {

    private final Outbox outbox = mock(Outbox.class);
    @SuppressWarnings("unchecked")
    private final ObjectProvider<Notifier> notifiers = mock(ObjectProvider.class);
    private final AktimetrixProperties properties = new AktimetrixProperties();
    private final Notifications notifications = new Notifications(outbox, notifiers, properties, JsonMapper.builder().build());

    @Test
    void aStepAtRiskOverdueOrCompletedLateIsNotified() {
        givenNotifiers(mock(Notifier.class));
        final StepInstance step = step(null);

        notifications.onStep(step, "AT_RISK", published("e1"));
        notifications.onStep(step, "OVERDUE", published("e2"));
        notifications.onStep(step, "COMPLETED", published("e3"));
        step.setTimeliness(Timeliness.LATE);
        notifications.onStep(step, "COMPLETED", published("e4"));
        notifications.onStep(step, "PLANNED", published("e5"));

        final ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(outbox, org.mockito.Mockito.times(3)).enqueue(eq(Notifications.DESTINATION), eq("p1"), sent.capture());
        assertThat(sent.getAllValues()).extracting(Notification::getId, Notification::getCondition)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("e1", "AT_RISK"),
                        org.assertj.core.groups.Tuple.tuple("e2", "OVERDUE"),
                        org.assertj.core.groups.Tuple.tuple("e4", "LATE"));
        assertThat(sent.getValue()).satisfies(n -> {
            assertThat(n.getSubject()).isEqualTo(Notification.STEP);
            assertThat(n.getEntityId()).isEqualTo("1234");
            assertThat(n.getProcessCode()).isEqualTo("ORDER_DELIVERY");
            assertThat(n.getStepCode()).isEqualTo("HANDOVER");
        });
    }

    @Test
    void nothingIsQueuedWithoutANotifierOrForAConditionNotChosen() {
        givenNotifiers();
        notifications.onStep(step(null), "AT_RISK", published("e1"));

        givenNotifiers(mock(Notifier.class));
        properties.getNotifications().getOn().remove("AT_RISK");
        notifications.onStep(step(null), "AT_RISK", published("e2"));

        verify(outbox, never()).enqueue(anyString(), any(), any());
    }

    @Test
    void aProcessPastItsDeadlineIsNotifiedWithItsRun() {
        givenNotifiers(mock(Notifier.class));
        final ProcessInstance process = new ProcessInstance();
        process.setId("p1");
        process.setProcessCode("ORDER_DELIVERY");
        process.setEntityId("1234");
        process.setRun(2);

        notifications.onProcess(process, "OVERDUE", published("e1"));

        final ArgumentCaptor<Notification> sent = ArgumentCaptor.forClass(Notification.class);
        verify(outbox).enqueue(eq(Notifications.DESTINATION), eq("p1"), sent.capture());
        assertThat(sent.getValue().getSubject()).isEqualTo(Notification.PROCESS);
        assertThat(sent.getValue().getRun()).isEqualTo(2);
    }

    private void givenNotifiers(Notifier... found) {
        when(notifiers.orderedStream()).thenAnswer(call -> Stream.of(found));
    }

    private static StepInstance step(Timeliness timeliness) {
        final StepInstance step = new StepInstance();
        step.setId("s1");
        step.setTenant("AA");
        step.setStepCode("HANDOVER");
        step.setProcessInstanceId("p1");
        step.setPlannedAt(LocalDateTime.of(2024, 1, 10, 10, 30).toInstant(ZoneOffset.UTC));
        step.setTimeliness(timeliness);
        return step;
    }

    private static Event<?, EventContext> published(String eventId) {
        final Event<Object, EventContext> event = new Event<>();
        event.setEventId(eventId);
        event.setEventDetails(EventContext.builder().processCode("ORDER_DELIVERY")
                .businessEntity(new EventContext.BusinessEntity("com.ecom.order", "1234")).build());
        return event;
    }
}
