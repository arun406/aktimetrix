package com.aktimetrix.core.configurations;

import com.aktimetrix.autoconfigure.AktimetrixDefaultProperties;
import com.aktimetrix.core.api.EventMapper;
import com.aktimetrix.core.event.handler.DefaultEventHandler;
import com.aktimetrix.core.exception.EventHandlerNotFoundException;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.service.AktimetrixMetrics;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.store.AktimetrixTransactions;
import com.aktimetrix.core.transferobjects.Event;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.ZonedDateTime;
import java.util.function.Consumer;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ProcessConfigTest {

    @Mock
    private RegistryService registryService;
    @Mock
    private DefaultEventHandler defaultEventHandler;
    @Mock
    private AktimetrixMetrics metrics;
    @Mock
    private AktimetrixTransactions transactions;
    @Mock
    private Outbox outbox;

    private final AktimetrixProperties properties = new AktimetrixProperties();

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            invocation.<Runnable>getArgument(0).run();
            return null;
        }).when(transactions).run(any());
        when(registryService.getEventHandler(anyString())).thenThrow(new EventHandlerNotFoundException("none"));
    }

    @Test
    void handlesEventsInTheApplicationsOwnFormat() {
        // a shop publishes {"id":"1234","status":"SHIPPED"}; the mapper turns it into an Aktimetrix event
        EventMapper shopEvents = (payload, headers) -> payload.contains("SHIPPED")
                ? Event.of("AA", "ORDER_SHIPPED_EVENT", "com.ecom.order", "1234", ZonedDateTime.now())
                : null;

        processor(shopEvents).accept(MessageBuilder.withPayload("{\"id\":\"1234\",\"status\":\"SHIPPED\"}").build());

        ArgumentCaptor<Event<?, ?>> handled = ArgumentCaptor.forClass(Event.class);
        verify(defaultEventHandler).handle(handled.capture());
        assertThat(handled.getValue().getEventCode()).isEqualTo("ORDER_SHIPPED_EVENT");
        assertThat(handled.getValue().getEntityId()).isEqualTo("1234");
        verify(metrics).eventReceived("AA", "ORDER_SHIPPED_EVENT", "handled");
    }

    @Test
    void ignoresMessagesTheMapperSkips() {
        processor((payload, headers) -> null).accept(MessageBuilder.withPayload("{\"status\":\"VIEWED\"}").build());

        verify(defaultEventHandler, never()).handle(any());
        verify(outbox, never()).enqueueRaw(any(), any(), any());
        verify(metrics).eventReceived(null, null, "ignored");
    }

    @Test
    void sendsMessagesTheMapperCannotReadToTheDeadLetterTopic() {
        processor((payload, headers) -> {
            throw new IllegalArgumentException("no status");
        }).accept(MessageBuilder.withPayload("{}").build());

        verify(defaultEventHandler, never()).handle(any());
        verify(outbox).enqueueRaw(AktimetrixDefaultProperties.DEAD_LETTER_BINDING, null, "{}");
        verify(metrics).eventReceived(null, null, "invalid");
    }

    private Consumer<org.springframework.messaging.Message<String>> processor(EventMapper mapper) {
        ProcessConfig config = new ProcessConfig();
        ReflectionTestUtils.setField(config, "eventMapper", mapper);
        ReflectionTestUtils.setField(config, "registryService", registryService);
        ReflectionTestUtils.setField(config, "defaultEventHandler", defaultEventHandler);
        ReflectionTestUtils.setField(config, "metrics", metrics);
        ReflectionTestUtils.setField(config, "transactions", transactions);
        ReflectionTestUtils.setField(config, "outbox", outbox);
        ReflectionTestUtils.setField(config, "properties", properties);
        return config.processor();
    }
}
