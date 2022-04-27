package com.aktimetrix.service.meter.core.meter.repository;

import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

public interface MeasurementInstanceRepository extends MongoRepository<MeasurementInstance, String> {

    /**
     * @param tenant            tenant
     * @param processInstanceId process instance id
     * @return list of measurement instance
     */
    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1 }")
    List<MeasurementInstance> findByProcessInstanceId(String tenant, ObjectId processInstanceId);

    /**
     * @param tenant            tenant
     * @param processInstanceId process instance id
     * @param type              measurement type
     * @return List of Measurement Instances
     */
    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1,  'type': ?2 }")
    List<MeasurementInstance> findByProcessInstanceIdAndType(String tenant, ObjectId processInstanceId, String type);


    /**
     * @param tenant            tenant
     * @param processInstanceId process instance id
     * @param stepInstanceId    step instance id
     * @return list of measurement instances
     */
    @Query(" { 'tenant': ?0 , 'processInstanceId': ?1 , 'stepInstanceId': ?2} ")
    List<MeasurementInstance> findByProcessInstanceIdAndStepInstanceId(String tenant, ObjectId processInstanceId, ObjectId stepInstanceId);
}
