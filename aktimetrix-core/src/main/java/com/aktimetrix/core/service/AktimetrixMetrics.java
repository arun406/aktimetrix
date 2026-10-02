package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.model.Alarm;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.DistributionSummary;
import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Metrics;
import io.micrometer.core.instrument.Timer;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.function.Supplier;

/**
 * Micrometer metrics of the monitored processes. They go to the application's {@link MeterRegistry} (for example
 * Prometheus through Spring Boot Actuator), or to Micrometer's global registry when there is none.
 * <p>
 * Tags are low-cardinality: tenant, process or step code, timeliness, outcome. Entity ids are never tags.
 *
 * <table>
 *     <tr><td>{@code aktimetrix.events}</td><td>counter</td><td>tenant, event, outcome</td></tr>
 *     <tr><td>{@code aktimetrix.processes.started}, {@code .completed}, {@code .cancelled}, {@code .overdue}</td><td>counter</td><td>tenant, process</td></tr>
 *     <tr><td>{@code aktimetrix.steps.completed}</td><td>counter</td><td>tenant, step, timeliness</td></tr>
 *     <tr><td>{@code aktimetrix.steps.lateness}</td><td>timer</td><td>tenant, step</td></tr>
 *     <tr><td>{@code aktimetrix.steps.overdue}, {@code .at.risk}</td><td>counter</td><td>tenant, step</td></tr>
 *     <tr><td>{@code aktimetrix.measurements.actual}</td><td>counter</td><td>tenant, measurement, conformance</td></tr>
 *     <tr><td>{@code aktimetrix.measurements.deviation}</td><td>distribution summary</td><td>tenant, measurement</td></tr>
 *     <tr><td>{@code aktimetrix.alarms.pending}</td><td>gauge</td><td></td></tr>
 *     <tr><td>{@code aktimetrix.alarms.fired}</td><td>counter</td><td>tenant, kind</td></tr>
 *     <tr><td>{@code aktimetrix.alarms.delay}</td><td>timer</td><td>kind</td></tr>
 *     <tr><td>{@code aktimetrix.outbox.pending}</td><td>gauge</td><td></td></tr>
 * </table>
 */
@Component
public class AktimetrixMetrics {

    private final MeterRegistry registry;

    public AktimetrixMetrics(ObjectProvider<MeterRegistry> registry) {
        this.registry = registry.getIfAvailable(() -> Metrics.globalRegistry);
    }

    /**
     * @param outcome {@code handled}, {@code ignored}, {@code invalid} or {@code failed}
     */
    public void eventReceived(String tenant, String eventCode, String outcome) {
        Counter.builder("aktimetrix.events").description("Business events received")
                .tag("tenant", value(tenant)).tag("event", value(eventCode)).tag("outcome", outcome)
                .register(registry).increment();
    }

    public void processStarted(ProcessInstance process) {
        processCounter("aktimetrix.processes.started", "Process instances started", process).increment();
    }

    public void processCompleted(ProcessInstance process) {
        processCounter("aktimetrix.processes.completed", "Process instances completed", process).increment();
    }

    public void processCancelled(ProcessInstance process) {
        processCounter("aktimetrix.processes.cancelled", "Process instances cancelled", process).increment();
    }

    public void processOverdue(ProcessInstance process) {
        processCounter("aktimetrix.processes.overdue", "Process instances that passed their deadline", process).increment();
    }

    public void stepCompleted(StepInstance step) {
        Counter.builder("aktimetrix.steps.completed").description("Steps completed, by timeliness")
                .tag("tenant", value(step.getTenant())).tag("step", value(step.getStepCode()))
                .tag("timeliness", step.getTimeliness() == null ? "unplanned" : step.getTimeliness().name())
                .register(registry).increment();
        if (step.getPlannedAt() != null && step.getActualAt() != null) {
            final Duration lateness = Duration.between(step.getPlannedAt(), step.getActualAt());
            Timer.builder("aktimetrix.steps.lateness").description("How long after its planned time a step completed")
                    .tag("tenant", value(step.getTenant())).tag("step", value(step.getStepCode()))
                    .register(registry).record(lateness.isNegative() ? Duration.ZERO : lateness);
        }
    }

    public void stepOverdue(StepInstance step) {
        stepCounter("aktimetrix.steps.overdue", "Steps that passed their deadline without their event", step).increment();
    }

    /**
     * A sign of poor event quality, such as a step that completed without its start event.
     *
     * @param issue e.g. {@code start_missing}
     */
    public void eventQuality(String tenant, String stepCode, String issue) {
        Counter.builder("aktimetrix.events.quality").description("Signs of missing or out-of-order business events")
                .tag("tenant", value(tenant)).tag("step", value(stepCode)).tag("issue", issue)
                .register(registry).increment();
    }

    public void stepAtRisk(StepInstance step) {
        stepCounter("aktimetrix.steps.at.risk", "Steps forecast to miss their deadline", step).increment();
    }

    /**
     * An actual measurement was recorded; counted by conformance, and its numeric deviation from plan recorded.
     */
    public void measurementRecorded(MeasurementInstance measurement) {
        Counter.builder("aktimetrix.measurements.actual").description("Actual measurements recorded, by conformance")
                .tag("tenant", value(measurement.getTenant())).tag("measurement", value(measurement.getCode()))
                .tag("conformance", measurement.getConformance() == null ? "none" : measurement.getConformance().name())
                .register(registry).increment();
        if (measurement.getDeviation() != null && !Constants.MEASUREMENT_CODE_TIME.equals(measurement.getCode())) {
            try {
                DistributionSummary.builder("aktimetrix.measurements.deviation")
                        .description("Actual minus planned value, in the measurement's unit")
                        .tag("tenant", value(measurement.getTenant())).tag("measurement", value(measurement.getCode()))
                        .register(registry).record(Double.parseDouble(measurement.getDeviation()));
            } catch (NumberFormatException e) {
                // not a number: nothing to record
            }
        }
    }

    public void alarmsPending(Supplier<Number> pending) {
        Gauge.builder("aktimetrix.alarms.pending", pending)
                .description("Alarms set at deadlines, not fired yet").register(registry);
    }

    public void alarmFired(Alarm alarm, Duration delay) {
        Counter.builder("aktimetrix.alarms.fired").description("Alarms fired on a step or process still open")
                .tag("tenant", value(alarm.getTenant())).tag("kind", value(alarm.getKind()))
                .register(registry).increment();
        Timer.builder("aktimetrix.alarms.delay").description("How long after its due time an alarm fired")
                .tag("kind", value(alarm.getKind()))
                .register(registry).record(delay.isNegative() ? Duration.ZERO : delay);
    }

    /**
     * Registers the gauge of outbox messages not yet sent to the message broker.
     */
    public void outboxPending(Supplier<Number> pending) {
        Gauge.builder("aktimetrix.outbox.pending", pending)
                .description("Outbox messages not yet sent to the message broker").register(registry);
    }

    private Counter processCounter(String name, String description, ProcessInstance process) {
        return Counter.builder(name).description(description)
                .tag("tenant", value(process.getTenant())).tag("process", value(process.getProcessCode()))
                .register(registry);
    }

    private Counter stepCounter(String name, String description, StepInstance step) {
        return Counter.builder(name).description(description)
                .tag("tenant", value(step.getTenant())).tag("step", value(step.getStepCode()))
                .register(registry);
    }

    private static String value(String tag) {
        return tag == null ? "none" : tag;
    }
}
