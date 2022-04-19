package com.aktimetrix.service.processor.core.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@org.springframework.data.mongodb.core.mapping.Document(collection = "stepInstances")
public class StepInstance {

    private String tenant;
    @Id
    private ObjectId id;
    private ObjectId processInstanceId;
    private String stepCode;
    private String locationCode;
    private String groupCode;
    private String status;
    private String version;
    private String functionalCtx;
    private Map<String, Object> metadata;
    private LocalDateTime createdOn;

    public StepInstance() {
        super();
    }

    /**
     * @param tenant
     * @param stepCode
     * @param processInstanceId
     * @param groupCode
     * @param functionalCtx
     * @param version
     * @param status
     * @param createdOn
     */
    public StepInstance(String tenant, String stepCode, ObjectId processInstanceId, String groupCode,
                        String functionalCtx, String version, String status, LocalDateTime createdOn) {
        this.tenant = tenant;
        this.stepCode = stepCode;
        this.processInstanceId = processInstanceId;
        this.groupCode = groupCode;
        this.functionalCtx = functionalCtx;
        this.version = version;
        this.status = status;
        this.createdOn = createdOn;
    }
}
