package com.aktimetrix.products.svm.core.planner.api;


import com.aktimetrix.products.svm.core.model.ProcessInstance;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.planner.UnableToCancelPlan;
import com.aktimetrix.products.svm.core.planner.UnableToCreatePlan;
import com.aktimetrix.products.svm.core.planner.UnableToModifyPlan;
import org.bson.types.ObjectId;

import java.util.List;

/**
 * @author arun kumar k
 */
public interface Planner {

    Plan createPlan(String tenant, ProcessInstance processInstance, List<StepInstance> stepInstances) throws UnableToCreatePlan;

    Plan updatePlan(String tenant, Plan plan, ProcessInstance processInstance, List<StepInstance> stepInstances) throws UnableToModifyPlan;

    void cancelPlan() throws UnableToCancelPlan;

    Plan getPlan(String tenant, ObjectId planId);
}
