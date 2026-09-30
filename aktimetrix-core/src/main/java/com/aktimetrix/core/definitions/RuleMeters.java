package com.aktimetrix.core.definitions;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.meter.api.ProcessMeter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;

import java.time.ZonedDateTime;

/**
 * Internal: turns the planning rules of the DSL into meters. Not part of the public API.
 */
public final class RuleMeters {

    private RuleMeters() {
    }

    public static Meter step(Definitions.Rule rule) {
        return new Meter() {
            @Override
            public MeasurementInstance measure(String tenant, StepInstance step) {
                final Object value = rule.getStepRule().apply(step);
                return value == null ? null : new MeasurementInstance(tenant, rule.getMeasurementCode(),
                        String.valueOf(value), rule.getUnit(), step.getProcessInstanceId(), step.getId(),
                        step.getStepCode(), Constants.PLAN_MEASUREMENT_TYPE, step.getLocationCode(),
                        ZonedDateTime.now());
            }

            @Override
            public String toString() {
                return "rule for " + rule.getMeasurementCode() + " of step " + rule.getStepCode();
            }
        };
    }

    public static ProcessMeter process(Definitions.Rule rule) {
        return new ProcessMeter() {
            @Override
            public MeasurementInstance measure(String tenant, ProcessInstance process) {
                final Object value = rule.getProcessRule().apply(process);
                return value == null ? null : new MeasurementInstance(tenant, rule.getMeasurementCode(),
                        String.valueOf(value), rule.getUnit(), process.getId(), null, null,
                        Constants.PLAN_MEASUREMENT_TYPE, null, ZonedDateTime.now());
            }

            @Override
            public String toString() {
                return "rule for " + rule.getMeasurementCode() + " of process " + rule.getProcessCode();
            }
        };
    }
}
