package com.aktimetrix.products.svm.core.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.javatuples.Pair;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Plan is collection of Steps with planned measurements map
 *
 * @author Arun kumar K
 */
@Data
@Document(collection = "plans")
public class Plan {

    private ObjectId id;
    private String tenant;
    private ObjectId processInstanceId;
    private String processCode;
    // step code and plan measurement map
    private Map<Pair, List<MeasurementInstance>> planMeasurements;
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
     * @param planMeasurements
     * @param createdOn
     * @param version
     * @param activeInd
     * @param status
     * @param completeInd
     */
    public Plan(String tenant, ObjectId processInstanceId, String processCode, Map<Pair,
            List<MeasurementInstance>> planMeasurements, LocalDateTime createdOn, String version, String activeInd,
                String status, String completeInd) {
        this.tenant = tenant;
        this.processInstanceId = processInstanceId;
        this.processCode = processCode;
        this.planMeasurements = planMeasurements;
        this.createdOn = createdOn;
        this.version = version;
        this.activeInd = activeInd;
        this.status = status;
        this.completeInd = completeInd;
    }

    @Override
    public String toString() {
        return "Plan{" +
                "id=" + id +
                ", tenant='" + tenant + '\'' +
                ", processInstanceId=" + processInstanceId +
                ", processCode='" + processCode + '\'' +
                ", planMeasurements=" + planMeasurements +
                ", createdOn=" + createdOn +
                ", version='" + version + '\'' +
                ", activeInd='" + activeInd + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
