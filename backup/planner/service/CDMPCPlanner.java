package com.aktimetrix.products.svm.ciq.cdmpc.planner.service;

import com.aktimetrix.products.svm.ciq.cdmpc.planner.util.MilestoneProcessor;
import com.aktimetrix.products.svm.ciq.cdmpc.planner.util.ShipmentCompletenessCheckService;
import com.aktimetrix.products.svm.ciq.cdmpc.service.CDMPCCommonUtil;
import com.aktimetrix.products.svm.core.Constants;
import com.aktimetrix.products.svm.core.model.Plan;
import com.aktimetrix.products.svm.core.model.ProcessInstance;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.planner.UnableToCancelPlan;
import com.aktimetrix.products.svm.core.planner.UnableToCreatePlan;
import com.aktimetrix.products.svm.core.planner.UnableToModifyPlan;
import com.aktimetrix.products.svm.core.planner.api.Planner;
import com.aktimetrix.products.svm.core.referencedata.model.StepDefinition;
import com.aktimetrix.products.svm.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.products.svm.core.service.PlannerService;
import com.aktimetrix.products.svm.core.util.BooleanUtils;
import com.aktimetrix.products.svm.core.util.DateTimeUtil;
import org.bson.types.ObjectId;
import org.javatuples.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * @author arun kumar k
 */
@Service
public class CDMPCPlanner implements Planner {

    private static final Logger logger = LoggerFactory.getLogger(CDMPCPlanner.class);

    @Autowired
    private CDMPCCommonUtil cdmpcCommonUtil;

    @Autowired
    private MilestoneProcessor milestoneProcessor;

    @Autowired
    private ShipmentCompletenessCheckService shipmentCompletenessCheckService;

    @Autowired
    private PlannerService plannerService;

    @Autowired
    private RouteMapService routeMapService;

    @Autowired
    private StepDefinitionService stepDefinitionService;

    /**
     * Creates the Plan
     *
     * @param tenant
     * @param stepInstances
     * @return
     * @throws UnableToCreatePlan
     */
    @Override
    public Plan createPlan(String tenant, ProcessInstance processInstance, List<StepInstance> stepInstances) throws UnableToCreatePlan {

        logger.info(" Tenant: {}, process instance id: {}", tenant, processInstance.getId());

        final List<StepInstance> steps = milestoneProcessor.filterPlannedMeasurements(stepInstances);
        final Map<Pair, List<MeasurementInstance>> stepMeasurementsMap = steps.stream()
                .collect(Collectors.toMap(x -> Pair.with(x.getStepCode(), x.getLocationCode()), StepInstance::getMeasurements));

        final boolean isComplete = this.shipmentCompletenessCheckService.isComplete(tenant, processInstance);
        logger.info("is process instance is complete: {}", isComplete);


        // Create and save Plan
        Plan plan = new Plan(tenant, processInstance.getId(), processInstance.getProcessCode(), stepMeasurementsMap,
                LocalDateTime.now(), Constants.DEFAULT_VERSION, "Y", Constants.CREATED, BooleanUtils.toStringYOrN(isComplete));

        plan = savePlan(plan);
        logger.info(" Plan is saved with Id : {} ", plan.getId());

        final Map<String, Object> metadata = processInstance.getMetadata();
        final String forwarderCode = (String) metadata.get("forwarderCode");
        final String origin = (String) metadata.get("origin");
        final String destination = (String) metadata.get("destination");
        logger.info("Forwarder Code: " + forwarderCode + ", Origin: " + origin + ", Destination: " + destination);
        final boolean partnerShipment = cdmpcCommonUtil.isPartnerShipment(tenant, tenant, forwarderCode, origin, destination);

        // create and save route map
        RouteMap routeMap = new RouteMap(tenant, plan.getId(), plan.getProcessInstanceId(), "N",
                BooleanUtils.toStringYOrN(partnerShipment), BooleanUtils.toStringYOrN(isComplete),
                "N", null, "N", Constants.CREATED, Constants.DEFAULT_VERSION);

        saveRouteMap(routeMap);
        logger.info(" Route Map is saved with Id : {} ", routeMap.getId());
        return plan;
    }

    /**
     * Saves the Route Map
     *
     * @param routeMap
     * @return
     */
    private RouteMap saveRouteMap(RouteMap routeMap) {
        this.routeMapService.saveRouteMap(routeMap);
        return routeMap;
    }

    /**
     * Update the plan
     *
     * @param tenant
     * @param plan
     * @param processInstance
     * @param stepInstances
     * @return
     * @throws UnableToModifyPlan
     */
    @Override
    public Plan updatePlan(String tenant, Plan plan,
                           ProcessInstance processInstance, List<StepInstance> stepInstances)
            throws UnableToModifyPlan {

        final Map<Pair, List<MeasurementInstance>> existingPlannedMeasurements = plan.getPlanMeasurements();

        final Map<Pair, List<MeasurementInstance>> stepMeasurementsMap = stepInstances.stream()
                .collect(Collectors.toMap(x -> Pair.with(x.getStepCode(), x.getLocationCode()), StepInstance::getMeasurements));
        // merge two measurement Maps

        final Iterator<Map.Entry<Pair, List<MeasurementInstance>>> iterator = stepMeasurementsMap.entrySet().iterator();
        while (iterator.hasNext()) {
            final Map.Entry<Pair, List<MeasurementInstance>> next = iterator.next();
            final String stepCode = (String) next.getKey().getValue0();
            final String locationCode = (String) next.getKey().getValue1();
            final List<MeasurementInstance> newMeasurementInstances = next.getValue();
            logger.info("Step Code: {}", stepCode);
            // Get Step Definition
            final StepDefinition stepDefinition = this.stepDefinitionService.findByStepCode(tenant, stepCode);
            if (stepDefinition == null) {
                throw new RuntimeException(String.format("Unable to Find step definition for %s", stepCode));
            }
            LocalDateTime newPlanTime = null, oldPlanTime = null;
            final MeasurementInstance newPlanTimeMi = getTimeMeasurementInstance(newMeasurementInstances);
            if (newPlanTimeMi != null) {
                newPlanTime = DateTimeUtil.getLocalDateTime(newPlanTimeMi);
                logger.info(" New Planned Time for the step {} is {}", newPlanTimeMi.getStepCode(), newPlanTime);
            }
            // check in existing plan
            final boolean isPresent = existingPlannedMeasurements.containsKey(Pair.with(stepCode, locationCode));
            logger.info(" is Measurements already exists for the step code {} at {} ", stepCode, locationCode);
            if (isPresent) {
                final List<MeasurementInstance> oldMeasurementInstances = existingPlannedMeasurements.get(Pair.with(stepCode, locationCode));
                MeasurementInstance oldPlanTimeMi = getTimeMeasurementInstance(oldMeasurementInstances);
                if (oldPlanTimeMi != null) {
                    // planned date
                    oldPlanTime = DateTimeUtil.getLocalDateTime(oldPlanTimeMi);
                    logger.info(" Old Planned Time for the step {} is {}", oldPlanTimeMi.getStepCode(), oldPlanTime);
                }
            }
            if (Constants.EXPORT_FUNCTION_CTX.equalsIgnoreCase(stepDefinition.getFunctionalCtxCode())) {
                if (!isPresent || (newPlanTime != null && newPlanTime.isBefore(oldPlanTime))) {
                    // Override with new Measurement Instance
                    existingPlannedMeasurements.put(Pair.with(stepCode, locationCode), newMeasurementInstances);
                }
            } else if (Constants.IMPORT_FUNCTION_CTX.equalsIgnoreCase(stepDefinition.getFunctionalCtxCode())) {
                if (!isPresent || (newPlanTime != null && newPlanTime.isAfter(oldPlanTime))) {
                    // Override with new Measurement Instances
                    existingPlannedMeasurements.put(Pair.with(stepCode, locationCode), newMeasurementInstances);
                }
            } else if (Constants.TRANSIT_FUNCTION_CTX.equalsIgnoreCase(stepDefinition.getFunctionalCtxCode())) {
                if (!isPresent) {
                    // Override with new Measurement Instances
                    existingPlannedMeasurements.put(Pair.with(stepCode, locationCode), newMeasurementInstances);
                } else {
                    // DEP-T

                    // ARR-T

                    // RCF-T
                }
            }
        }
        // update the plan with new measurements
        plan.setPlanMeasurements(existingPlannedMeasurements);

        final boolean isComplete = this.shipmentCompletenessCheckService.isComplete(tenant, processInstance);
        logger.info("is process instance is complete: {}", isComplete);
        plan.setCompleteInd(BooleanUtils.toStringYOrN(isComplete));
        savePlan(plan);
        logger.info(" Plan Id: {}", plan.getId());
        return plan;
    }


    /**
     * @param measurementInstances
     * @return
     */
    private MeasurementInstance getTimeMeasurementInstance(List<MeasurementInstance> measurementInstances) {
        if (measurementInstances != null && !measurementInstances.isEmpty()) {
            return measurementInstances.stream()
                    .filter(mi -> mi.getCode().equalsIgnoreCase("TIME")).findFirst().orElse(null);
        }
        return null;
    }

    @Override
    public void cancelPlan() throws UnableToCancelPlan {
        //TODO cancel the Plan
    }

    @Override
    public Plan getPlan(String tenant, ObjectId plamId) {
        return this.plannerService.findPlanByProcessInstanceId(tenant, plamId);
    }


    /**
     * Saves the Plan
     *
     * @param plan
     * @return
     */
    private Plan savePlan(Plan plan) {
        this.plannerService.save(plan);
        return plan;
    }
}
