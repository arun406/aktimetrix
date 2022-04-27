package com.aktimetrix.products.svm.core.service;

import com.aktimetrix.products.svm.core.model.Plan;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PlannerService {


    @Autowired
    private PlanRepository repository;

    /**
     * @param plan
     */
    public Plan save(Plan plan) {
        return this.repository.save(plan);
    }

    /**
     * @param tenant
     * @param id
     * @return
     */
    public Plan findPlanByProcessInstanceId(String tenant, ObjectId id) {
        return this.repository.findPlanByProcessInstanceId(tenant, id);
    }

}
