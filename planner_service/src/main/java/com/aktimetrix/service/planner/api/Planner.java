package com.aktimetrix.service.planner.api;


import com.aktimetrix.service.planner.model.Plan;
import com.aktimetrix.service.planner.model.ProcessInstance;

import java.util.List;

/**
 * @author arun kumar kandakatla
 */
public interface Planner {

    Plan createPlan(String tenant, ProcessInstance processInstance);

    Plan updatePlan(String tenant, Plan plan, ProcessInstance processInstance);

    void cancelPlan();

    Plan getPlan(String tenant, String planId);

    List<Plan> getPlans(String tenant, String entityId, String entityType);

    List<Plan> getPlans(String tenant, String entityId, String entityType, String status);

    List<Plan> getActivePlans(String tenant, String entityId, String entityType);

    List<Plan> getActivePlans(String tenant, String entityId, String entityType, String version);

    List<Plan> getPlans(String tenant, String processInstanceId);

    Plan getActivePlan(String tenant, String processInstanceId);

    List<Plan> getAllActivePlans(String tenant);

    void activatePlan(String tenant, String planId);

    void deactivatePlan(String tenant, String planId);

    boolean isActive(String tenant, String planId);

}
