package com.aktimetrix.products.svm.ciq.cdmpc.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@NoArgsConstructor
@Document("bkdMeasurementInstances")
public class BKDMeasurementInstance {

    private String tenant;
    private ObjectId id;
    private ObjectId processId;
    private ObjectId stepId;
    private String code;
    private org.bson.Document value;


    /**
     * @param tenant
     * @param code
     * @param value
     * @param processId
     * @param stepId
     */
    public BKDMeasurementInstance(String tenant, String code, org.bson.Document value, ObjectId processId,
                                  ObjectId stepId) {
        this.tenant = tenant;
        this.code = code;
        this.value = value;
        this.processId = processId;
        this.stepId = stepId;
    }
}
