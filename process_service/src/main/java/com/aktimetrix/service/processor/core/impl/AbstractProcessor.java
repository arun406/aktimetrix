package com.aktimetrix.service.processor.core.impl;

import com.aktimetrix.service.processor.core.api.ProcessContext;
import com.aktimetrix.service.processor.core.api.Processor;
import com.aktimetrix.service.processor.core.service.ProcessInstancePublisherService;
import com.aktimetrix.service.processor.core.service.StepInstancePublisherService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public abstract class AbstractProcessor implements Processor {

    private final ProcessInstancePublisherService processInstancePublisherService;
    private final StepInstancePublisherService stepInstancePublisherService;

    @Autowired
    public AbstractProcessor(ProcessInstancePublisherService processInstancePublisherService, StepInstancePublisherService stepInstancePublisherService) {
        this.processInstancePublisherService = processInstancePublisherService;
        this.stepInstancePublisherService = stepInstancePublisherService;
    }

    /**
     * @param context process context
     */
    @Override
    public void process(ProcessContext context) {
        // call pre processors
        executePreProcessors(context);
        // call do process
        doProcess(context);
        // post processors
        executePostProcessors(context);
    }

    public abstract void doProcess(ProcessContext context);

    private void executePreProcessors(ProcessContext context) {
        // get the preprocessors from registry
        log.debug("executing the preprocessors");
    }

    /**
     * Execute the post processor
     *
     * @param context process context
     */
    private void executePostProcessors(ProcessContext context) {
        // TODO  get the post processors from registry
        log.debug("executing post processors");
        // publish the process instance event
        this.processInstancePublisherService.process(context);
        this.stepInstancePublisherService.process(context);
    }
}
