package com.aktimetrix.service.planner.impl;

import com.aktimetrix.service.planner.Constants;
import com.aktimetrix.service.planner.api.Planner;
import com.aktimetrix.service.planner.model.Plan;
import com.aktimetrix.service.planner.model.ProcessInstance;
import com.aktimetrix.service.planner.service.MeasurementInstanceService;
import com.aktimetrix.service.planner.service.PlannerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;

@Component
public class DefaultPlannerImpl implements Planner {
    final private static Logger logger = LoggerFactory.getLogger(DefaultPlannerImpl.class);

    @Autowired
    private PlannerService plannerService;

    @Autowired
    private MeasurementInstanceService measurementInstanceService;

    @Override
    public Plan createPlan(String tenant, ProcessInstance processInstance) {

        Plan plan = new Plan(tenant, processInstance.getId(), processInstance.getProcessCode(),
                LocalDateTime.now(), Constants.DEFAULT_VERSION, "Y", Constants.STATUS_CREATED);

        savePlan(plan);
        logger.info("Plan is saved with Id : {} and version {}", plan.getId(), plan.getVersion());
        return plan;
    }

    @Override
    public Plan updatePlan(String tenant, Plan plan, ProcessInstance processInstance) {
        return null;
    }

    @Override
    public void cancelPlan() {
    }

    @Override
    public Plan getPlan(String tenant, String planId) {
        return null;
    }


    @Override
    public List<Plan> getPlans(String tenant, String entityId, String entityType) {
        return null;
    }

    @Override
    public List<Plan> getPlans(String tenant, String entityId, String entityType, String status) {
        return null;
    }

    @Override
    public List<Plan> getActivePlans(String tenant, String entityId, String entityType) {
        return null;
    }

    @Override
    public List<Plan> getActivePlans(String tenant, String entityId, String entityType, String version) {
        return null;
    }

    @Override
    public List<Plan> getPlans(String tenant, String processInstanceId) {
        return null;
    }

    @Override
    public Plan getActivePlan(String tenant, String processInstanceId) {
        return null;
    }

    @Override
    public List<Plan> getAllActivePlans(String tenant) {
        return null;
    }

    @Override
    public void activatePlan(String tenant, String planId) {

    }

    @Override
    public void deactivatePlan(String tenant, String planId) {

    }

    @Override
    public boolean isActive(String tenant, String planId) {
        return false;
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
