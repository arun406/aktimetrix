package com.aktimetrix.service.planner.transferobjects;

import com.aktimetrix.service.planner.model.MeasurementInstance;
import com.google.common.collect.ImmutableListMultimap;
import com.google.common.collect.Multimap;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;

import java.time.LocalDateTime;

/**
 * Plan is series of Steps with planned measurements map order by some criteria
 *
 * @author Arun kumar K
 */
@Data
@NoArgsConstructor
public class PlanInstanceDTO {

    private ObjectId id;
    private String tenant;
    private ProcessInstanceDTO processInstance;
    // step code and plan measurement map
    private Multimap<String, MeasurementInstanceDTO> stepMeasurements;
    private LocalDateTime createdOn;
    private String version;
    private String activeInd;
    private String completeInd;
    private String status;

    /**
     * Constructor
     *
     * @param tenant
     * @param stepMeasurements
     * @param createdOn
     * @param version
     * @param activeInd
     * @param status
     */
    public PlanInstanceDTO(String tenant,
                           Multimap<String, MeasurementInstanceDTO> stepMeasurements, LocalDateTime createdOn,
                           String version, String activeInd, String status) {
        this.tenant = tenant;
        this.stepMeasurements = stepMeasurements;
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
                ", createdOn=" + createdOn +
                ", version='" + version + '\'' +
                ", activeInd='" + activeInd + '\'' +
                ", status='" + status + '\'' +
                '}';
    }
}
