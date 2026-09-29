package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.MeasurementType;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.meter.api.ProcessMeter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.MeasurementDefinition;
import com.aktimetrix.core.transferobjects.Event;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

/**
 * Records the actual ({@code A}) measurements of a step or process when it completes, in any dimension: read from
 * the completing event's entity when the measurement says where ({@code valueFrom}), otherwise computed by the meter
 * registered for it. Each is then compared with its plan by {@link MeasurementComparison}. The actual {@code TIME} of
 * a step is recorded separately, always.
 */
@Service
@RequiredArgsConstructor
public class ActualMeasurementService {
    private static final Logger logger = LoggerFactory.getLogger(ActualMeasurementService.class);
    private static final ObjectMapper ENTITY_READER = new ObjectMapper().findAndRegisterModules();

    private final RegistryService registryService;
    private final Clock clock;
    private final MeasurementComparison comparison;

    /**
     * @param event the event that completed the step; may be {@code null}
     */
    public List<MeasurementInstance> forStep(StepInstance step, List<MeasurementDefinition> declared, Event<?, ?> event) {
        final List<MeasurementInstance> actuals = new ArrayList<>();
        for (MeasurementDefinition measurement : actualsOnly(declared)) {
            final MeasurementInstance actual;
            if (measurement.getValueFrom() != null) {
                final Object value = read(event, measurement.getValueFrom());
                actual = value == null ? null : new MeasurementInstance(step.getTenant(), measurement.getMeasurementCode(),
                        String.valueOf(value), measurement.getUnit(), step.getProcessInstanceId(), step.getId(),
                        step.getStepCode(), Constants.ACTUAL_MEASUREMENT_TYPE, step.getLocationCode(), ZonedDateTime.now(clock));
            } else {
                final Meter meter = registryService.getMeter(step.getTenant(), step.getStepCode(),
                        measurement.getMeasurementCode());
                actual = meter == null ? null : meter.measureActual(step.getTenant(), step, event);
            }
            add(actuals, actual, measurement, step.getStepCode(), declared);
        }
        return actuals;
    }

    /**
     * Interim readings of an open step, from a progress event: its actual measurements that the event carries
     * ({@code valueFrom}), each compared with the plan.
     */
    public List<MeasurementInstance> readings(StepInstance step, List<MeasurementDefinition> declared, Event<?, ?> event) {
        final List<MeasurementInstance> readings = new ArrayList<>();
        for (MeasurementDefinition measurement : actualsOnly(declared)) {
            if (measurement.getValueFrom() == null) {
                continue;
            }
            final Object value = read(event, measurement.getValueFrom());
            if (value == null) {
                continue;
            }
            final MeasurementInstance reading = new MeasurementInstance(step.getTenant(), measurement.getMeasurementCode(),
                    String.valueOf(value), measurement.getUnit(), step.getProcessInstanceId(), step.getId(),
                    step.getStepCode(), Constants.ACTUAL_MEASUREMENT_TYPE, step.getLocationCode(), ZonedDateTime.now(clock));
            reading.setInterim(true);
            add(readings, reading, measurement, step.getStepCode(), declared);
        }
        return readings;
    }

    /**
     * @param event the event that completed the process; may be {@code null}
     */
    public List<MeasurementInstance> forProcess(ProcessInstance process, List<MeasurementDefinition> declared,
                                                Event<?, ?> event) {
        final List<MeasurementInstance> actuals = new ArrayList<>();
        for (MeasurementDefinition measurement : actualsOnly(declared)) {
            final MeasurementInstance actual;
            if (measurement.getValueFrom() != null) {
                final Object value = read(event, measurement.getValueFrom());
                actual = value == null ? null : new MeasurementInstance(process.getTenant(),
                        measurement.getMeasurementCode(), String.valueOf(value), measurement.getUnit(), process.getId(),
                        null, null, Constants.ACTUAL_MEASUREMENT_TYPE, null, ZonedDateTime.now(clock));
            } else {
                final ProcessMeter meter = registryService.getProcessMeter(process.getTenant(), process.getProcessCode(),
                        measurement.getMeasurementCode());
                actual = meter == null ? null : meter.measureActual(process.getTenant(), process, event);
            }
            add(actuals, actual, measurement, process.getProcessCode(), declared);
        }
        return actuals;
    }

    private static List<MeasurementDefinition> actualsOnly(List<MeasurementDefinition> declared) {
        final List<MeasurementDefinition> actuals = new ArrayList<>();
        if (declared != null) {
            for (MeasurementDefinition measurement : declared) {
                // the actual TIME of a step is always recorded, from its event's time
                if (MeasurementType.A == measurement.getType()
                        && !Constants.MEASUREMENT_CODE_TIME.equals(measurement.getMeasurementCode())) {
                    actuals.add(measurement);
                }
            }
        }
        return actuals;
    }

    private void add(List<MeasurementInstance> actuals, MeasurementInstance actual, MeasurementDefinition measurement,
                     String owner, List<MeasurementDefinition> declared) {
        if (actual != null) {
            final String code = measurement.getMeasurementCode();
            comparison.compare(actual, declaredField(declared, code, MeasurementDefinition::getTolerance),
                    declaredField(declared, code, MeasurementDefinition::getWorseWhen));
            actuals.add(actual);
        } else {
            logger.debug("No actual {} recorded for {}", measurement.getMeasurementCode(), owner);
        }
    }

    /**
     * A field declared for the code, on its planned or its actual measurement, e.g. the tolerance.
     */
    private static String declaredField(List<MeasurementDefinition> declared, String code,
                                        Function<MeasurementDefinition, String> field) {
        for (MeasurementDefinition measurement : declared) {
            if (code.equals(measurement.getMeasurementCode()) && field.apply(measurement) != null) {
                return field.apply(measurement);
            }
        }
        return null;
    }

    /**
     * Reads a dot-separated path, e.g. {@code delivery.distanceKm}, in the event's entity.
     */
    static Object read(Event<?, ?> event, String path) {
        if (event == null || event.getEntity() == null) {
            return null;
        }
        Object current = event.getEntity() instanceof Map ? event.getEntity()
                : ENTITY_READER.convertValue(event.getEntity(), Map.class);
        for (String key : path.split("\\.")) {
            if (!(current instanceof Map)) {
                return null;
            }
            current = ((Map<?, ?>) current).get(key);
        }
        return current;
    }
}
