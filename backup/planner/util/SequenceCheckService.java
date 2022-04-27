package com.aktimetrix.products.svm.ciq.cdmpc.planner.util;

import com.aktimetrix.products.svm.ciq.cdmpc.service.CDMPCCommonUtil;
import com.aktimetrix.products.svm.core.model.ProcessInstance;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.service.StepInstanceService;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.stream.Collectors;

@Component
public class SequenceCheckService {

    private static final Logger logger = LoggerFactory.getLogger(SequenceCheckService.class);

    private final CDMPCCommonUtil commonUtil;
    private final StepInstanceService stepInstanceService;
    private final RouteBuilder routeBuilder;

    /**
     * @param commonUtil
     * @param routeBuilder
     */
    public SequenceCheckService(StepInstanceService stepInstanceService, CDMPCCommonUtil commonUtil, RouteBuilder routeBuilder) {
        this.commonUtil = commonUtil;
        this.routeBuilder = routeBuilder;
        this.stepInstanceService = stepInstanceService;
    }

    /**
     * @param processInstance
     */
    public void check(String tenant, ProcessInstance processInstance) {

        // generate route string based on the itineraries
        final String origin = (String) processInstance.getMetadata().get("origin");
        final String destination = (String) processInstance.getMetadata().get("destination");

        final List<StepInstance> stepInstances = this.stepInstanceService.getStepInstancesByProcessInstanceId(tenant, processInstance.getId());
        if (processInstance.isComplete()) {
            final List<Document> itineraries = commonUtil.getItinerariesFromProcessInstance(stepInstances);

            final List<StepInstance> list = stepInstances.stream()
                    .filter(stepInstance -> !stepInstance.getStepCode().equalsIgnoreCase("BKD"))
                    .collect(Collectors.toList());

            routeBuilder.getAllRouteMaps(itineraries, list, origin, destination);
        }
    }
}
