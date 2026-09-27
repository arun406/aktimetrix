package com.aktimetrix.core.model;

import com.aktimetrix.core.api.Timeliness;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
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
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId processInstanceId;
    private String stepCode;
    /**
     * Position of the step in its process, from 0.
     */
    private int sequence;
    private String locationCode;
    private String groupCode;
    private String status;
    private String version;
    private String functionalCtx;
    private Map<String, Object> metadata;
    private LocalDateTime createdOn;
    /**
     * When the step should happen: the planned TIME measurement computed by its meter, if any.
     */
    private LocalDateTime plannedAt;
    /**
     * When the step counts as late: {@code plannedAt} plus the step definition's tolerance.
     */
    private LocalDateTime lateAfter;
    /**
     * Forecast of when the step will happen, when an earlier step ran late.
     */
    private LocalDateTime expectedAt;
    /**
     * When the step actually completed.
     */
    private LocalDateTime actualAt;
    /**
     * How the step compares with its plan; {@code null} until it can be judged.
     */
    private Timeliness timeliness;

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
