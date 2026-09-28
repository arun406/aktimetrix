package com.aktimetrix.core.service;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import io.micrometer.core.instrument.Counter;
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
 *     <tr><td>{@code aktimetrix.processes.started}</td><td>counter</td><td>tenant, process</td></tr>
 *     <tr><td>{@code aktimetrix.processes.completed}</td><td>counter</td><td>tenant, process</td></tr>
 *     <tr><td>{@code aktimetrix.steps.completed}</td><td>counter</td><td>tenant, step, timeliness</td></tr>
 *     <tr><td>{@code aktimetrix.steps.lateness}</td><td>timer</td><td>tenant, step</td></tr>
 *     <tr><td>{@code aktimetrix.steps.overdue}</td><td>counter</td><td>tenant, step</td></tr>
 *     <tr><td>{@code aktimetrix.steps.at.risk}</td><td>counter</td><td>tenant, step</td></tr>
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

    public void stepAtRisk(StepInstance step) {
        stepCounter("aktimetrix.steps.at.risk", "Steps forecast to miss their deadline", step).increment();
    }

    /**
     * Registers the gauge of outbox messages not yet sent to Kafka.
     */
    public void outboxPending(Supplier<Number> pending) {
        Gauge.builder("aktimetrix.outbox.pending", pending)
                .description("Outbox messages not yet sent to Kafka").register(registry);
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
