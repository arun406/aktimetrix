package com.aktimetrix.core.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@ToString
@Document(collection = "stepInstances")
public class StepInstance {

    private String tenant;
    @Id
    private String id;
    private String processInstanceId;
    private String stepCode;
    private String locationCode;
    private String groupCode;
    private String status;
    private String version;
    private String functionalCtx;
    private Map<String, Object> metadata;
    private LocalDateTime createdOn;    // UTC
    private LocalDateTime modifiedOn;   // UTC
    private List<MeasurementInstance> measurements;

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
    public StepInstance(String tenant, String stepCode, String processInstanceId, String groupCode,
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

    /**
     * @param tenant
     * @param stepCode
     * @param groupCode
     * @param functionalCtx
     * @param version
     * @param status
     * @param createdOn
     */
    public StepInstance(String tenant, String stepCode, String groupCode,
                        String functionalCtx, String version, String status, LocalDateTime createdOn) {
        this.tenant = tenant;
        this.stepCode = stepCode;
        this.groupCode = groupCode;
        this.functionalCtx = functionalCtx;
        this.version = version;
        this.status = status;
        this.createdOn = createdOn;
    }
}
