package com.aktimetrix.service.processor.core.service;

import com.aktimetrix.service.processor.core.api.PostProcessor;
import com.aktimetrix.service.processor.core.api.ProcessContext;
import com.aktimetrix.service.processor.core.impl.StepEventGenerator;
import com.aktimetrix.service.processor.core.transferobjects.Event;
import com.aktimetrix.service.processor.core.transferobjects.StepInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StepInstancePublisherService implements PostProcessor {

    final private StreamBridge streamBridge;

    @Override
    public void postProcess(ProcessContext context) {
        log.debug("executing process instance publisher service");
        if (context.getStepInstances() == null || context.getStepInstances().isEmpty()) {
            return;
        }
        context.getStepInstances().forEach(step -> {
            StepEventGenerator eventGenerator = new StepEventGenerator(step);
            Event<StepInstanceDTO, Void> event = eventGenerator.generate();
            log.debug("step instance event : {}", event);
            this.streamBridge.send("step-instance-out-0", event);
        });
    }
}
