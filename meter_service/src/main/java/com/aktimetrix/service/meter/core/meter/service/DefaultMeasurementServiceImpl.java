package com.aktimetrix.service.meter.core.meter.service;

import com.aktimetrix.service.meter.core.api.Constants;
import com.aktimetrix.service.meter.core.api.Registry;
import com.aktimetrix.service.meter.core.meter.api.MeasurementService;
import com.aktimetrix.service.meter.core.meter.api.Meter;
import com.aktimetrix.service.meter.core.model.MeasurementInstance;
import com.aktimetrix.service.meter.core.transferobjects.Measurement;
import com.aktimetrix.service.meter.core.transferobjects.StepInstanceDTO;
import com.aktimetrix.service.meter.core.util.CollectionUtil;
import com.aktimetrix.service.meter.referencedata.model.StepDefinition;
import com.aktimetrix.service.meter.referencedata.model.StepMeasurement;
import com.aktimetrix.service.meter.referencedata.service.StepDefinitionService;
import com.aktimetrix.service.meter.referencedata.transferobjects.MeasurementType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@RequiredArgsConstructor
@Slf4j
/**
 *
 * Change this to DefaultMeasurementServiceImpl //TODO
 */
public class DefaultMeasurementServiceImpl implements MeasurementService {

    private final Registry registry;
    private final StepDefinitionService stepDefinitionService;
    @Autowired
    private MeasurementInstanceService measurementInstanceService;

    /**
     * Return applicable meter instance
     *
     * @param tenant          tenant parameter
     * @param stepCode        step code
     * @param measurementCode measurement code
     */
    private Meter getMeter(String tenant, String stepCode, String measurementCode) {
        final List<Object> meters = this.registry.lookupAll(registryEntry ->
                registryEntry.hasAttribute(Constants.ATT_METER_SERVICE) &&
                        registryEntry.attribute(Constants.ATT_METER_SERVICE).equals(Constants.VAL_YES) &&
                        (registryEntry.attribute(Constants.ATT_CODE).equals(measurementCode)
                                && registryEntry.attribute(Constants.ATT_STEP_CODE).equals(stepCode))
        );
        Meter meter = null;
        for (Object m : meters) {
            meter = (Meter) m;
        }
        return meter;
    }

    /**
     * Generate measurements
     *
     * @param tenantKey tenant
     * @param step      step
     * @return measurement instance
     */
    public List<Measurement> generateMeasurements(String tenantKey, StepInstanceDTO step) {
        final String stepCode = step.getStepCode();
        // Get the applicable steps in the process
        // get the measurement codes
        log.debug("Tenant : {}, step code : {}, measurement code: {}", tenantKey, stepCode, "TIME");

        final Map<String, Object> metadata = step.getMetadata();
        if (metadata != null)
            metadata.forEach((key, value) -> log.debug("key :{} value :{}", key, value));
        // applicable step definitions
        log.debug("finding applicable step definition for the {} step", stepCode);
        StepDefinition stepDefinition = getStepDefinition(tenantKey, step.getStepCode());

        if (stepDefinition != null && !CollectionUtil.isEmptyOrNull(stepDefinition.getMeasurements())) {
            List<MeasurementInstance> measurements = new ArrayList<>();
            for (StepMeasurement m : stepDefinition.getMeasurements()) {
                if (MeasurementType.P == m.getType()) {
                    log.info("Step Code: {}, Measurement Code: {} ", stepDefinition.getStepCode(), m.getMeasurementCode());
                    Meter meter = getMeter(tenantKey, stepDefinition.getStepCode(), m.getMeasurementCode());
                    if (meter != null) {
                        final MeasurementInstance measurement = meter.measure(tenantKey, step);
                        log.debug("measurement instance found for " + meter.getClass().getName());
                        measurements.add(measurement);
                    } else {
                        log.info(" Meter is not defined for this step : {} and measurement code : {}",
                                stepDefinition.getStepCode(), m.getMeasurementCode());
                    }
                }
            }
            if (!measurements.isEmpty()) {
                this.measurementInstanceService.saveMeasurementInstances(measurements);
                return measurements.stream().map(this::getMeasurement).collect(Collectors.toList());
            }
        }
        return new ArrayList<>();
    }

    /**
     * Returns Step Definition
     *
     * @param tenant tenant
     * @param code   step code
     * @return step definition
     */
    private StepDefinition getStepDefinition(String tenant, String code) {
        return this.stepDefinitionService.findByStepCode(tenant, code);
    }

    public com.aktimetrix.service.meter.core.transferobjects.Measurement getMeasurement(MeasurementInstance mi) {
        return Measurement.builder()
                .type(mi.getType())
                .tenant(mi.getTenant())
                .code(mi.getCode())
                .id(mi.getId().toString())
                .measuredAt(mi.getMeasuredAt())
                .createdOn(mi.getCreatedOn())
                .stepCode(mi.getStepCode())
                .processInstanceId(mi.getProcessInstanceId().toString())
                .stepInstanceId(mi.getStepInstanceId().toString())
                .unit(mi.getUnit())
                .value(mi.getValue())
                .build();
    }
}
