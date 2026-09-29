package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.impl.MeasurementEventGenerator;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.transferobjects.Event;
import com.aktimetrix.core.transferobjects.EventContext;
import com.aktimetrix.core.transferobjects.Measurement;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
@com.aktimetrix.core.stereotypes.PostProcessor(code = "MI_PUBLISHER", processType = "METERPROCESSOR")
public class MeasurementInstancePublisherService implements PostProcessor {

    final private Outbox outbox;
    final private PublishedEventContexts contexts;

    /**
     * Publishes a {@code Measurement_Event}, keyed by the id of the measurement's process instance, so that a
     * process's measurement events stay in order.
     */
    public void publish(MeasurementInstance measurement) {
        final Event<Measurement, EventContext> event =
                new MeasurementEventGenerator(measurement, contexts.of(measurement)).generate();
        log.debug("measurement instance event : {}", event);
        outbox.enqueue("measurement-instance-out-0", measurement.getProcessInstanceId(), event);
    }

    @Override
    public void postProcess(Context context) {
        log.debug("executing process instance publisher service");
        if (context.getMeasurementInstances() != null) {
            context.getMeasurementInstances().forEach(this::publish);
        }
    }
}
