package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.service.StepInstanceService;
import org.bson.types.ObjectId;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AbstractProcessorTest {

    @Mock
    private StepInstanceService stepInstanceService;
    @Mock
    private ProcessInstanceService processInstanceService;
    @InjectMocks
    private OrderProcessor processor;

    @Test
    void replayedStartEventReusesExistingProcessAndSteps() {
        ProcessInstance existing = new ProcessInstance();
        existing.setId(new ObjectId());
        StepInstance place = new StepInstance();
        when(processInstanceService.getProcessInstance("AA", "ORDER_DELIVERY", "com.ecom.order", "1234"))
                .thenReturn(existing);
        when(stepInstanceService.getStepInstancesByProcessInstanceId("AA", existing.getId())).thenReturn(List.of(place));

        DefaultContext context = new DefaultContext();
        context.setTenant("AA");
        context.setProperty(Constants.ENTITY_ID, "1234");
        ProcessDefinition definition = new ProcessDefinition("AA", "ORDER_DELIVERY");
        definition.setEntityType("com.ecom.order");
        context.setProperty(Constants.PROCESS_DEFINITION, definition);

        processor.doProcess(context);

        verify(processInstanceService, never()).saveProcessInstance(any());
        verify(stepInstanceService, never()).save(anyString(), any(), any(), any());
        assertThat(context.getProcessInstance()).isSameAs(existing);
        assertThat(existing.getSteps()).containsExactly(place);
        assertThat(context.getStepInstances()).isEmpty();
    }

    static class OrderProcessor extends AbstractProcessor {
        @Override
        protected Map<String, Object> getStepMetadata(Context context) {
            return Map.of();
        }

        @Override
        protected Map<String, Object> getProcessMetadata(Context context) {
            return Map.of();
        }
    }
}
