package com.aktimetrix.core.event.handler;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.DefaultProcessor;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.service.StepProgressService;
import com.aktimetrix.core.transferobjects.Event;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class DefaultEventHandlerTest {

    private static final Instant PLACED_AT = LocalDateTime.of(2022, 5, 22, 23, 46).toInstant(ZoneOffset.UTC);

    @Mock
    private ProcessDefinitionService processDefinitionService;
    @Mock
    private RegistryService registryService;
    @Mock
    private DefaultProcessor defaultProcessor;
    @Mock
    private StepProgressService stepProgressService;
    @InjectMocks
    private DefaultEventHandler handler;

    @Test
    void startsProcessWithItsHandlerThenRecordsTheMilestone() throws Exception {
        Event<Object, Object> event = event("ORDER_PLACED_EVENT");
        ProcessDefinition delivery = new ProcessDefinition("AA", "ORDER_DELIVERY");
        Processor orderProcessor = mock(Processor.class);
        when(processDefinitionService.findStartedBy("AA", "ORDER_PLACED_EVENT")).thenReturn(List.of(delivery));
        when(registryService.getProcessHandler("ORDER_DELIVERY")).thenReturn(orderProcessor);
        when(stepProgressService.occurredAt(event)).thenReturn(PLACED_AT);

        handler.handle(event);

        InOrder order = inOrder(orderProcessor, stepProgressService);
        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        order.verify(orderProcessor).process(context.capture());
        order.verify(stepProgressService).recordMilestones("AA", "com.ecom.order", "1234", "ORDER_PLACED_EVENT", PLACED_AT, event);
        assertThat(context.getValue().getProcessType()).as("process type defaults to the process code")
                .isEqualTo("ORDER_DELIVERY");
        verify(defaultProcessor, never()).process(any());
    }

    @Test
    void usesDefaultProcessorWhenProcessHasNoHandler() throws Exception {
        Event<Object, Object> event = event("ORDER_PLACED_EVENT");
        ProcessDefinition delivery = new ProcessDefinition("AA", "ORDER_DELIVERY");
        delivery.setProcessType("DELIVERY");
        when(processDefinitionService.findStartedBy("AA", "ORDER_PLACED_EVENT")).thenReturn(List.of(delivery));
        when(registryService.getProcessHandler("ORDER_DELIVERY")).thenThrow(new ProcessHandlerNotFoundException());

        handler.handle(event);

        ArgumentCaptor<Context> context = ArgumentCaptor.forClass(Context.class);
        verify(defaultProcessor).process(context.capture());
        assertThat(context.getValue().getProcessType()).isEqualTo("DELIVERY");
    }

    @Test
    void eventThatStartsNothingIsStillAMilestone() {
        Event<Object, Object> event = event("ORDER_SHIPPED_EVENT");
        when(processDefinitionService.findStartedBy("AA", "ORDER_SHIPPED_EVENT")).thenReturn(List.of());
        when(stepProgressService.occurredAt(event)).thenReturn(PLACED_AT);

        handler.handle(event);

        verify(stepProgressService).recordMilestones("AA", "com.ecom.order", "1234", "ORDER_SHIPPED_EVENT", PLACED_AT, event);
    }

    private static Event<Object, Object> event(String code) {
        Event<Object, Object> event = new Event<>();
        event.setTenantKey("AA");
        event.setEventCode(code);
        event.setEntityType("com.ecom.order");
        event.setEntityId("1234");
        return event;
    }
}
