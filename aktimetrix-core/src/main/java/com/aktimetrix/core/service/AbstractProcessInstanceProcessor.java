package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.MultiplePostProcessFoundException;
import com.aktimetrix.core.exception.PostProcessorNotFoundException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public abstract class AbstractProcessInstanceProcessor implements Processor {

    final protected RegistryService registryService;
    final protected ProcessInstanceService processInstanceService;
    final protected ObjectMapper objectMapper;

    public AbstractProcessInstanceProcessor(RegistryService registryService,
                                            ProcessInstanceService processInstanceService, ObjectMapper objectMapper) {
        this.registryService = registryService;
        this.processInstanceService = processInstanceService;
        this.objectMapper = objectMapper;
    }

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

    protected void executePostProcessors(Context context) {
        final PostProcessor publisher;
        try {
            publisher = registryService.getPostProcessor("PPI_PUBLISHER");
            log.debug("publishing the process plan instance..");
            publisher.process(context);
        } catch (PostProcessorNotFoundException e) {
            log.error(e.getMessage(), e);
        } catch (MultiplePostProcessFoundException e) {
            log.error(e.getMessage(), e);
        }
    }
}
