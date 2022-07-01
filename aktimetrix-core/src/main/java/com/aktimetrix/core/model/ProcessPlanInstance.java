package com.aktimetrix.core.model;

import com.google.common.collect.ArrayListMultimap;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Plan is series of Steps with planned measurements map order by some criteria
 *
 * @author Arun kumar K
 */
@Data
@Document(collection = "processPlanInstances")
@NoArgsConstructor
public class ProcessPlanInstance {

    @Id
    private String id;
    private String tenant;
    private String processInstanceId;
    private String processCode;
    private String processType;
    private LocalDateTime createdOn;
    private LocalDateTime modifiedOn;
    private int version;
    private String activeInd;
    private String completeInd;
    private String status;
    private boolean rmpSent;
    private String entityId;
    private String entityType;
    private String shipmentIndicator;
    private String approvedIndicator;
    private String directTruckingIndicator;
    private String phaseNumber;
    private int planNumber;
    private String flightSpecificIndicator;
    private boolean valid;
    private ArrayListMultimap<String, StepPlanInstance> stepPlanInstances;
    private String updateFlag;

    /**
     * Constructor
     *
     * @param tenant
     * @param processInstanceId
     * @param processCode
     * @param createdOn
     * @param activeInd
     * @param status
     * @param completeInd
     */
    public ProcessPlanInstance(String tenant, String processInstanceId, String processCode, LocalDateTime createdOn,
                               String activeInd, String status, String completeInd, String entityId, String entityType) {
        this.tenant = tenant;
        this.processInstanceId = processInstanceId;
        this.processCode = processCode;
        this.createdOn = createdOn;
        this.activeInd = activeInd;
        this.status = status;
        this.completeInd = completeInd;
        this.entityId = entityId;
        this.entityType = entityType;
    }

    /**
     * @param tenant
     * @param processInstanceId
     * @param processCode
     * @param createdOn
     * @param activeInd
     * @param status
     * @param completeInd
     * @param entityId
     * @param entityType
     * @param version
     * @param planNumber
     */
    public ProcessPlanInstance(String tenant, String processInstanceId, String processCode, String processType, LocalDateTime createdOn,
                               String activeInd, String status, String completeInd, String entityId, String entityType, int version, int planNumber) {
        this.tenant = tenant;
        this.processInstanceId = processInstanceId;
        this.processCode = processCode;
        this.processType = processType;
        this.createdOn = createdOn;
        this.activeInd = activeInd;
        this.status = status;
        this.completeInd = completeInd;
        this.entityId = entityId;
        this.entityType = entityType;
        this.version = version;
        this.planNumber = planNumber;
    }

    @Override
    public String toString() {
        return "Plan{" +
                "id=" + id +
                ", tenant='" + tenant + '\'' +
                ", processInstanceId=" + processInstanceId +
                ", processCode='" + processCode + '\'' +
                ", createdOn=" + createdOn +
                ", version='" + version + '\'' +
                ", activeInd='" + activeInd + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
