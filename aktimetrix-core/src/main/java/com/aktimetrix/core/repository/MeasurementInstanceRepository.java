package com.aktimetrix.core.repository;

import com.aktimetrix.core.model.MeasurementInstance;
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
    /**
     * Measurements of one step, or of the process itself when {@code stepInstanceId} is {@code null}, with the code
     * and type.
     */
    @Query("{ 'tenant': ?0, 'processInstanceId': ?1, 'stepInstanceId': ?2, 'code': ?3, 'type': ?4 }")
    List<MeasurementInstance> findByOwnerAndCodeAndType(String tenant, ObjectId processInstanceId, ObjectId stepInstanceId,
                                                        String code, String type);

    @Query(" { 'tenant': ?0 , 'processInstanceId': ?1 , 'stepInstanceId': ?2} ")
    List<MeasurementInstance> findByProcessInstanceIdAndStepInstanceId(String tenant, ObjectId processInstanceId, ObjectId stepInstanceId);
}
