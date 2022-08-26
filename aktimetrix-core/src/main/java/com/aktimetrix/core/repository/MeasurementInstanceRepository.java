package com.aktimetrix.core.repository;

import com.aktimetrix.core.model.MeasurementInstance;
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
    List<MeasurementInstance> findByProcessInstanceId(String tenant, String processInstanceId);

    /**
     * @param tenant            tenant
     * @param processInstanceId process instance id
     * @param type              measurement type
     * @return List of Measurement Instances
     */
    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1,  'type': ?2 }")
    List<MeasurementInstance> findByProcessInstanceIdAndType(String tenant, String processInstanceId, String type);


    /**
     * @param tenant            tenant
     * @param processInstanceId process instance id
     * @param stepInstanceId    step instance id
     * @return list of measurement instances
     */
    @Query(" { 'tenant': ?0 , 'processInstanceId': ?1 , 'stepInstanceId': ?2 } ")
    List<MeasurementInstance> findByProcessInstanceIdAndStepInstanceId(String tenant, String processInstanceId, String stepInstanceId);

    /**
     * @param tenant
     * @param processInstanceId
     * @param stepInstanceId
     * @param measurementType
     * @return
     */
    @Query(" { 'tenant': ?0 , 'processInstanceId': ?1 , 'stepInstanceId': ?2, 'type' : ?3} ")
    List<MeasurementInstance> findByProcessInstanceIdAndStepInstanceIdAndType(String tenant, String processInstanceId, String stepInstanceId, String measurementType);

    /**
     * @param tenant
     * @param entityId
     * @param entityType
     * @param stepCode
     * @return
     */
    @Query("{ 'tenant' : ?0, 'metadata.documentNumber', ?1, 'metadata.documentType': ?2 , 'stepCode': ?3 , 'type': 'A' }")
    List<MeasurementInstance> findActualByEntityIdAndEntityTypeAndStepCode(String tenant, String entityId, String entityType, String stepCode);
}
