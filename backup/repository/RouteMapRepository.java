package com.aktimetrix.products.svm.ciq.cdmpc.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

public interface RouteMapRepository extends MongoRepository<RouteMap, String> {

    @Query("{ 'tenant' : ?0, 'planId': ?1 }")
    RouteMap findByPlanId(String tenant, ObjectId planId);

}
