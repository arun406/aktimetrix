package com.aktimetrix.core.referencedata.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.util.List;

@Data
@ToString
@NoArgsConstructor
@Document(collection = "processDefinitions")
public class ProcessDefinition {
    private String tenant;
    @Id
    private String id;
    private String processType;
    private String processCode;
    private String processName;
    private String processDescription;
    private String categoryCode;
    private String subCategoryCode;
    private List<String> tags;
    private String entityType;
    private List<String> startEventCodes;
    /**
     * Events that cancel a running instance of the process, e.g. {@code ORDER_CANCELLED_EVENT}: the process and its
     * open steps become {@code Cancelled}, and are no longer monitored.
     */
    private List<String> cancelEventCodes;
    private String status;
    private String responsiblePartyCode;
    private String groupCode;
    private List<StepDefinition> steps;
    /**
     * Measurements of the process as a whole, e.g. its total distance; planned ones are computed by
     * process-level meters when the process instance is created.
     */
    private List<MeasurementDefinition> measurements;
    /**
     * ISO-8601 duration within which the whole process should complete, from its start, e.g. {@code P1D}.
     * Optional; gives the process its own deadline and timeliness.
     */
    private String plannedWithin;
    /**
     * ISO-8601 duration the process may run past its planned completion before it counts as late or overdue.
     */
    private String tolerance;

    /**
     * @param tenant
     * @param processCode
     */
    public ProcessDefinition(String tenant, String processCode) {
        this.tenant = tenant;
        this.processCode = processCode;
    }

    public Duration plannedWithinDuration() {
        return plannedWithin == null ? null : Duration.parse(plannedWithin);
    }

    public Duration toleranceDuration() {
        return tolerance == null ? Duration.ZERO : Duration.parse(tolerance);
    }
}
