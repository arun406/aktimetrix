package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.service.StepProgressService;
import com.aktimetrix.core.transferobjects.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractMilestoneEventHandlerTest {

    @Mock
    private StepProgressService stepProgressService;
    @InjectMocks
    private OrderShippedEventHandler handler;

    @Test
    void recordsTheEventAsMilestoneOfTheEntity() {
        Event<Object, Object> event = new Event<>();
        event.setTenantKey("AA");
        event.setEventCode("ORDER_SHIPPED_EVENT");
        event.setEntityType("com.ecom.order");
        event.setEntityId("1234");
        LocalDateTime shippedAt = LocalDateTime.of(2022, 5, 23, 1, 30);
        when(stepProgressService.occurredAt(event)).thenReturn(shippedAt);

        handler.handle(event);

        verify(stepProgressService).recordMilestones("AA", "com.ecom.order", "1234", "ORDER_SHIPPED_EVENT", shippedAt, event);
    }

    static class OrderShippedEventHandler extends AbstractMilestoneEventHandler {
    }
}
