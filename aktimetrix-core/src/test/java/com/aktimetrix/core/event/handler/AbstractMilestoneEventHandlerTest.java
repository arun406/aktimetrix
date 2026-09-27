package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.service.StepProgressService;
import com.aktimetrix.core.transferobjects.Event;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractMilestoneEventHandlerTest {

    @Mock
    private ProcessInstanceService processInstanceService;
    @Mock
    private StepProgressService stepProgressService;
    @InjectMocks
    private OrderShippedEventHandler handler;

    @Test
    void recordsMilestoneForEveryActiveProcessOfTheEntity() {
        ProcessInstance delivery = new ProcessInstance();
        delivery.setId(new ObjectId());
        ProcessInstance billing = new ProcessInstance();
        billing.setId(new ObjectId());
        when(processInstanceService.getActiveProcessInstances("AA", "com.ecom.order", "1234"))
                .thenReturn(List.of(delivery, billing));

        handler.handle(shippedEvent());

        LocalDateTime shippedAt = LocalDateTime.of(2022, 5, 23, 1, 30);
        verify(stepProgressService).recordMilestone("ORDER_SHIPPED_EVENT", delivery, shippedAt);
        verify(stepProgressService).recordMilestone("ORDER_SHIPPED_EVENT", billing, shippedAt);
    }

    @Test
    void ignoresEventWithoutActiveProcess() {
        when(processInstanceService.getActiveProcessInstances("AA", "com.ecom.order", "1234")).thenReturn(List.of());

        handler.handle(shippedEvent());

        verify(stepProgressService, never()).recordMilestone(any(), any(), any());
    }

    private static Event<Object, Object> shippedEvent() {
        Event<Object, Object> event = new Event<>();
        event.setTenantKey("AA");
        event.setEventCode("ORDER_SHIPPED_EVENT");
        event.setEntityType("com.ecom.order");
        event.setEntityId("1234");
        event.setEventUTCTime(LocalDateTime.of(2022, 5, 23, 1, 30));
        return event;
    }

    static class OrderShippedEventHandler extends AbstractMilestoneEventHandler {
    }
}
