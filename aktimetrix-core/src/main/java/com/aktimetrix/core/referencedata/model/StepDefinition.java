package com.aktimetrix.core.referencedata.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;

import java.time.Duration;
import java.util.List;

@Data
@ToString
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
    /**
     * Events that report progress while the step is open, e.g. {@code LOCATION_UPDATED} during a journey: each records
     * interim readings of the step's actual measurements that it carries ({@code valueFrom}), compared with the plan,
     * without completing the step.
     */
    private List<String> progressEventCodes;
    private String groupCode;
    private List<MeasurementDefinition> measurements;
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

    /**
     * This definition with every field that {@code override} sets replacing its own; lists are replaced as a whole.
     * Used to let a process adapt a step shared by several processes, e.g. a shorter {@code plannedWithin} for an
     * express delivery.
     */
    public StepDefinition overriddenBy(StepDefinition override) {
        final StepDefinition merged = new StepDefinition();
        merged.setTenant(tenant);
        merged.setId(id);
        merged.setStepCode(stepCode);
        merged.setStepName(pick(override.getStepName(), stepName));
        merged.setOptionalInd(pick(override.getOptionalInd(), optionalInd));
        merged.setCategoryCode(pick(override.getCategoryCode(), categoryCode));
        merged.setSubCategoryCode(pick(override.getSubCategoryCode(), subCategoryCode));
        merged.setStatus(pick(override.getStatus(), status));
        merged.setFunctionalCtxCode(pick(override.getFunctionalCtxCode(), functionalCtxCode));
        merged.setLocationCtxCode(pick(override.getLocationCtxCode(), locationCtxCode));
        merged.setResponsiblePartyCode(pick(override.getResponsiblePartyCode(), responsiblePartyCode));
        merged.setStartEventCodes(pick(override.getStartEventCodes(), startEventCodes));
        merged.setEndEventCodes(pick(override.getEndEventCodes(), endEventCodes));
        merged.setProgressEventCodes(pick(override.getProgressEventCodes(), progressEventCodes));
        merged.setGroupCode(pick(override.getGroupCode(), groupCode));
        merged.setMeasurements(pick(override.getMeasurements(), measurements));
        merged.setPlannedAfter(pick(override.getPlannedAfter(), plannedAfter));
        merged.setPlannedWithin(pick(override.getPlannedWithin(), plannedWithin));
        merged.setTolerance(pick(override.getTolerance(), tolerance));
        return merged;
    }

    private static <T> T pick(T override, T base) {
        return override != null ? override : base;
    }
}
