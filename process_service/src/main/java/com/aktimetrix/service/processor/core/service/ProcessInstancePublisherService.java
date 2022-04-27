package com.aktimetrix.service.processor.core.service;

import com.aktimetrix.service.processor.core.api.PostProcessor;
import com.aktimetrix.service.processor.core.api.ProcessContext;
import com.aktimetrix.service.processor.core.impl.ProcessEventGenerator;
import com.aktimetrix.service.processor.core.transferobjects.Event;
import com.aktimetrix.service.processor.core.transferobjects.ProcessInstanceDTO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cloud.stream.function.StreamBridge;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProcessInstancePublisherService implements PostProcessor {

    final private StreamBridge streamBridge;

    @Override
    public void postProcess(ProcessContext context) {
        log.debug("executing process instance publisher service");
        ProcessEventGenerator eventGenerator = new ProcessEventGenerator(context.getProcessInstance());
        final Event<ProcessInstanceDTO, Void> event = eventGenerator.generate();
        log.debug("process instance event : {}", event);
        this.streamBridge.send("process-instance-out-0", event);
    }
}
