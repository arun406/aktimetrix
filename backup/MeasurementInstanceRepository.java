package com.aktimetrix.products.svm.core.repository;

import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MeasurementInstanceRepository extends MongoRepository<MeasurementInstance, String> {

    /**
     * @param tenant
     * @param processInstanceId
     * @return
     */
    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1 }")
    List<MeasurementInstance> findByProcessInstanceId(String tenant, ObjectId processInstanceId);

    /**
     * @param tenant
     * @param processInstanceId
     * @param type
     * @return
     */
    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1,  'type': ?2 }")
    List<MeasurementInstance> findByProcessInstanceIdAndType(String tenant, ObjectId processInstanceId, String type);

    @Query(" { 'tenant': ?0 , 'processInstanceId': ?1 , 'stepInstanceId': ?2} ")
    List<MeasurementInstance> findByProcessInstanceIdAndStepInstanceId(String tenant, ObjectId processInstanceId, ObjectId stepInstanceId);
}
