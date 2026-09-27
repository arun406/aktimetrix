package com.aktimetrix.core.referencedata.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.Duration;
import java.util.List;

@Data
@ToString
@Document(collection = "stepDefinitions")
public class StepDefinition {

    private String tenant;
    @Id
    private String id;
    private String stepCode;
    private String stepName;
    private String optionalInd;
    private String categoryCode;
    private String subCategoryCode;
    private String status;
    private String functionalCtxCode;
    private String locationCtxCode;
    private String responsiblePartyCode;
    private List<String> startEventCodes;
    private List<String> endEventCodes;
    private String groupCode;
    private List<StepMeasurement> measurements;
    /**
     * Plans the step without a meter: its planned time is {@code plannedWithin} after the step
     * {@code plannedAfter} completes, or after the process starts when {@code plannedAfter} is absent.
     */
    private String plannedAfter;
    /**
     * ISO-8601 duration, e.g. {@code PT2H}. See {@link #plannedAfter}.
     */
    private String plannedWithin;
    /**
     * ISO-8601 duration the step may run past its planned time before it counts as late or overdue, e.g.
     * {@code PT15M}. Defaults to none.
     */
    private String tolerance;

    public Duration plannedWithinDuration() {
        return plannedWithin == null ? null : Duration.parse(plannedWithin);
    }

    public Duration toleranceDuration() {
        return tolerance == null ? Duration.ZERO : Duration.parse(tolerance);
    }
}
