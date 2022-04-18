package com.aktimetrix.products.svm.core.repository;

import com.aktimetrix.products.svm.core.model.Plan;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

/**
 * @author arun kumar kandakatla
 */
public interface PlanRepository extends MongoRepository<Plan, String> {

    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1 }")
    Plan findPlanByProcessInstanceId(String tenant, ObjectId id);
}
