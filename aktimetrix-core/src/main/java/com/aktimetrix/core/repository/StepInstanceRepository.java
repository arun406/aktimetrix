package com.aktimetrix.core.repository;

import com.aktimetrix.core.model.StepInstance;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;

import java.util.List;

/**
 * @author arun kumar kandakatla
 */
public interface StepInstanceRepository extends MongoRepository<StepInstance, String> {

    /**
     * @param tenant
     * @param stepCode
     * @param processInstanceId
     * @return
     */
    @Query("{ 'tenant' : ?0 , 'stepCode': ?1 , 'processInstanceId': ?2 }")
    List<StepInstance> findByTenantAndStepCodeAndProcessInstanceId(String tenant, String stepCode, String processInstanceId);

    /**
     * @param tenant
     * @param processInstanceId
     * @return
     */

    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1 }")
    List<StepInstance> findByTenantAndProcessInstanceId(String tenant, String processInstanceId);

    /**
     * @param tenant
     * @param processInstanceId
     * @param stepInstanceId
     * @return
     */
    @Query("{ 'tenant' : ?0 , 'processInstanceId': ?1, '_id': ?2 }")
    StepInstance getStepInstanceByProcessInstanceIdAndId(String tenant, String processInstanceId, String stepInstanceId);


    @Aggregation(pipeline = {"{\n" +
            "    $match: {\n" +
            "        tenant: ?0,\n" +
            "        processInstanceId: ?1,\n" +
            "        _id: ?2 \n" +
            "    }\n" +
            "}", "{\n" +
            "    $lookup: {\n" +
            "        from: 'measurementInstances',\n" +
            "        localField: '_id',\n" +
            "        foreignField: 'stepInstanceId',\n" +
            "        as: 'measurements'\n" +
            "    }\n" +
            "}",
            "{\n" +
                    "    $project: {\n" +
                    "        _class: 0,\n" +
                    "        'measurements._class': 0,\n" +
                    "        'measurements.processInstanceId': 0,\n" +
                    "        'measurements.stepInstanceId': 0\n" +
                    "    }\n" +
                    "}"
    })
    StepInstance getStepInstancesWithMeasurements(String tenant, String processInstanceId, String stepInstanceId);
}
