package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.Processor;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Slf4j
public abstract class AbstractProcessInstanceProcessor implements Processor {

    @Autowired
    private RegistryService registryService;
    @Autowired
    protected ProcessInstanceService processInstanceService;
    @Autowired
    protected ObjectMapper objectMapper;

    /**
     * @param context process context
     */
    @Override
    public void process(Context context) {
        // call pre processors
        log.debug("executing the pre processors");
        executePreProcessors(context);
        // call the processor
        doProcess(context);
        // post processors
        log.debug("executing post processors");
        executePostProcessors(context);
    }

    protected abstract void doProcess(Context context);

    protected void executePreProcessors(Context context) {
    }

    private void executePostProcessors(Context context) {
        final List<PostProcessor> publisher = registryService.getPostProcessor("PPI_PUBLISHER");
        if (!publisher.isEmpty()) {
            log.debug("publishing the process plan instance..");
            publisher.forEach(postProcessor -> postProcessor.process(context));
        }
    }
}
