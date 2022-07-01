package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.stereotypes.ProcessHandler;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.Map;

@Component
@Slf4j
@ProcessHandler(processCode = Constants.PROCESS_INSTANCE_CANCEL, processType = Constants.PROCESS_INSTANCE_TYPE, version = Constants.DEFAULT_VERSION)
public class ProcessInstanceCancellationProcessor implements Processor {

    @Autowired
    private ProcessInstanceService processInstanceService;

    /**
     * @param context process context
     */
    @Override
    public void process(Context context) {
        // call pre processors
        executePreProcessors(context);
        // call do process
        doProcess(context);
        // post processors
        executePostProcessors(context);
    }

    private void executePostProcessors(Context context) {
        log.debug("executing post processors");
    }

    private void doProcess(Context context) {
        ProcessInstance processInstance = context.getProcessInstance();
        log.debug("process instance id :{}", processInstance.getId());
        Map<String, Object> eventData = (Map<String, Object>) context.getProperty("eventData");
        String cancellationReason = (String) eventData.get("cancellationReason");
        log.debug("cancellation reason: {}", cancellationReason);
        processInstanceService.cancelProcessInstance(context.getTenant(), processInstance.getId(), cancellationReason);
    }

    private void executePreProcessors(Context context) {
        log.debug("executing pre processors");
    }
}
