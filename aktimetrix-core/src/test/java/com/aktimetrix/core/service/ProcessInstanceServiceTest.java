package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.store.ProcessInstanceStore;
import com.aktimetrix.core.store.StepInstanceStore;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProcessInstanceServiceTest {

    private final ProcessInstanceStore store = mock(ProcessInstanceStore.class);
    private final ProcessInstanceService service = new ProcessInstanceService(store, mock(StepInstanceStore.class),
            mock(DeadlineAlarms.class));

    @Test
    void eventsApplyToTheLatestRunOfEachProcessUnlessItWasCancelled() {
        final ProcessInstance delivery1 = instance("1", "DELIVERY", 1, Constants.STATUS_COMPLETED);
        final ProcessInstance delivery2 = instance("2", "DELIVERY", 2, "Created");
        final ProcessInstance returns1 = instance("3", "RETURNS", 1, Constants.STATUS_COMPLETED);
        final ProcessInstance returns2 = instance("4", "RETURNS", 2, Constants.STATUS_CANCELLED);
        final ProcessInstance otherType = instance("5", "DELIVERY", 3, "Created");
        otherType.setEntityType("com.ecom.parcel");
        when(store.findByEntityId("AA", "1234")).thenReturn(List.of(delivery2, delivery1, returns1, returns2, otherType));

        assertThat(service.getCurrentRuns("AA", "com.ecom.order", "1234")).containsExactly(delivery2);
        assertThat(service.getRuns("AA", "DELIVERY", "com.ecom.order", "1234")).containsExactly(delivery1, delivery2);
    }

    private static ProcessInstance instance(String id, String processCode, int run, String status) {
        final ProcessInstance instance = new ProcessInstance();
        instance.setId(id);
        instance.setTenant("AA");
        instance.setProcessCode(processCode);
        instance.setEntityType("com.ecom.order");
        instance.setEntityId("1234");
        instance.setRun(run);
        instance.setStatus(status);
        return instance;
    }
}
