package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.MeasurementType;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.meter.api.ProcessMeter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepMeasurement;
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

/**
 * Records the actual ({@code A}) measurements of a step or process when it completes, in any dimension: read from
 * the completing event's entity when the measurement says where ({@code valueFrom}), otherwise computed by the meter
 * registered for it. The actual {@code TIME} of a step is recorded separately, always.
 */
@Service
@RequiredArgsConstructor
public class ActualMeasurementService {
    private static final Logger logger = LoggerFactory.getLogger(ActualMeasurementService.class);
    private static final ObjectMapper ENTITY_READER = new ObjectMapper().findAndRegisterModules();

    private final RegistryService registryService;
    private final Clock clock;

    /**
     * @param event the event that completed the step; may be {@code null}
     */
    public List<MeasurementInstance> forStep(StepInstance step, List<StepMeasurement> declared, Event<?, ?> event) {
        final List<MeasurementInstance> actuals = new ArrayList<>();
        for (StepMeasurement measurement : actualsOnly(declared)) {
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
            add(actuals, actual, measurement, step.getStepCode());
        }
        return actuals;
    }

    /**
     * @param event the event that completed the process; may be {@code null}
     */
    public List<MeasurementInstance> forProcess(ProcessInstance process, List<StepMeasurement> declared,
                                                Event<?, ?> event) {
        final List<MeasurementInstance> actuals = new ArrayList<>();
        for (StepMeasurement measurement : actualsOnly(declared)) {
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
            add(actuals, actual, measurement, process.getProcessCode());
        }
        return actuals;
    }

    private static List<StepMeasurement> actualsOnly(List<StepMeasurement> declared) {
        final List<StepMeasurement> actuals = new ArrayList<>();
        if (declared != null) {
            for (StepMeasurement measurement : declared) {
                // the actual TIME of a step is always recorded, from its event's time
                if (MeasurementType.A == measurement.getType()
                        && !Constants.MEASUREMENT_CODE_TIME.equals(measurement.getMeasurementCode())) {
                    actuals.add(measurement);
                }
            }
        }
        return actuals;
    }

    private static void add(List<MeasurementInstance> actuals, MeasurementInstance actual, StepMeasurement measurement,
                            String owner) {
        if (actual != null) {
            actuals.add(actual);
        } else {
            logger.debug("No actual {} recorded for {}", measurement.getMeasurementCode(), owner);
        }
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
