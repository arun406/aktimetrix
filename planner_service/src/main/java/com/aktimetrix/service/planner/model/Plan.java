package com.aktimetrix.service.planner.model;

import com.google.common.collect.ImmutableListMultimap;
import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

/**
 * Plan is series of Steps with planned measurements map order by some criteria
 *
 * @author Arun kumar K
 */
@Data
@Document(collection = "plan-instance")
public class Plan {

    private ObjectId id;
    private String tenant;
    private ObjectId processInstanceId;
    private String processCode;
    // step code and plan measurement map
    private LocalDateTime createdOn;
    private String version;
    private String activeInd;
    private String completeInd;
    private String status;

    /**
     * Constructor
     *
     * @param tenant
     * @param processInstanceId
     * @param processCode
     * @param createdOn
     * @param version
     * @param activeInd
     * @param status
     */
    public Plan(String tenant, ObjectId processInstanceId, String processCode, LocalDateTime createdOn,
                String version, String activeInd, String status) {
        this.tenant = tenant;
        this.processInstanceId = processInstanceId;
        this.processCode = processCode;
        this.createdOn = createdOn;
        this.version = version;
        this.activeInd = activeInd;
        this.status = status;
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
