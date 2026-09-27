package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.impl.MeasurementEventGenerator;
import com.aktimetrix.core.outbox.Outbox;
import com.aktimetrix.core.transferobjects.Event;
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

    @Override
    public void postProcess(Context context) {
        log.debug("executing process instance publisher service");
        if (context.getMeasurementInstances() != null) {
            context.getMeasurementInstances().forEach(m -> {
                MeasurementEventGenerator eventGenerator = new MeasurementEventGenerator(m);
                Event<Measurement, Void> event = eventGenerator.generate();
                log.debug("measurement instance event : {}", event);
                outbox.enqueue("measurement-instance-out-0", event.getEntityId(), event);
            });
        }
    }
}
