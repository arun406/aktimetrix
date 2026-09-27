package com.aktimetrix.core.service;

import com.aktimetrix.core.api.*;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.model.StepMeasurement;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.stereotypes.ProcessHandler;
import com.aktimetrix.core.util.CollectionUtil;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Computes the planned measurements of one step instance: for every planned (P) measurement in the step's
 * definition, calls the meter registered for that step and measurement code. A planned TIME measurement also
 * becomes the step's {@code plannedAt}, which the monitor compares with the actual time.
 */
@Component
@RequiredArgsConstructor
@ProcessHandler(processType = Constants.METER_PROCESSOR)
public class DefaultMeasurementProcessor implements Processor {
    final private Logger logger = LoggerFactory.getLogger(DefaultMeasurementProcessor.class);

    final private StepDefinitionService stepDefinitionService;
    final private MeasurementInstanceService measurementInstanceService;
    final private StepInstanceService stepInstanceService;
    final private RegistryService registryService;

    /**
     * @param context process context
     */
    @Override
    public void process(Context context) {
        // call pre processors
        executePreProcessors(context);
        // call do process
        doProcess(context);
        // post processors
        executePostProcessors(context);
    }

    private void executePreProcessors(Context context) {
        // get the preprocessors from registry
        logger.debug("executing the preprocessors");

        final List<PreProcessor> preProcessors = registryService.getPreProcessor(Constants.METER_PROCESSOR, false);
        preProcessors.forEach(preProcessor -> preProcessor.process(context));
    }

    /**
     * Execute the post processor
     *
     * @param context process context
     */
    private void executePostProcessors(Context context) {
        logger.debug("executing post processors");
        final List<PostProcessor> postProcessors = registryService.getPostProcessor(Constants.METER_PROCESSOR, false);
        postProcessors.forEach(postProcessor -> postProcessor.process(context));
    }

    /**
     * Generate measurements
     */
    public void doProcess(Context context) {
        final StepInstance stepInstance = context.getStepInstances().get(0);
        final String stepCode = stepInstance.getStepCode();
        final Map<String, Object> metadata = stepInstance.getMetadata();
        if (metadata != null)
            metadata.forEach((key, value) -> logger.debug("key :{} value :{}", key, value));

        // applicable step definitions
        logger.debug("finding applicable step definition for the {} step", stepCode);
        StepDefinition stepDefinition = getStepDefinition(context.getTenant(), stepInstance.getStepCode());

        if (stepDefinition != null && !CollectionUtil.isEmptyOrNull(stepDefinition.getMeasurements())) {
            List<MeasurementInstance> measurementInstances = new ArrayList<>();
            for (StepMeasurement stepMeasurement : stepDefinition.getMeasurements()) {
                MeasurementInstance measurement = measure(context, stepInstance, stepDefinition, stepMeasurement);
                if (measurement != null) {
                    measurementInstances.add(measurement);
                }
            }
            if (!measurementInstances.isEmpty()) {
                this.measurementInstanceService.saveMeasurementInstances(measurementInstances);
                context.setMeasurementInstances(measurementInstances);
                recordPlannedTime(stepInstance, measurementInstances);
            }
        }
    }

    private void recordPlannedTime(StepInstance step, List<MeasurementInstance> measurements) {
        measurements.stream()
                .filter(m -> Constants.MEASUREMENT_CODE_TIME.equals(m.getCode()))
                .findFirst()
                .ifPresent(time -> {
                    try {
                        step.setPlannedAt(LocalDateTime.parse(time.getValue()));
                        stepInstanceService.save(step);
                    } catch (DateTimeParseException e) {
                        logger.warn("Planned TIME of the {} step is not an ISO date-time: {}", step.getStepCode(),
                                time.getValue());
                    }
                });
    }

    private MeasurementInstance measure(Context context, StepInstance stepInstance, StepDefinition stepDefinition,
                                        StepMeasurement stepMeasurement) {
        if (MeasurementType.P == stepMeasurement.getType()) {
            logger.info("Step Code: {}, Measurement Code: {} ", stepDefinition.getStepCode(), stepMeasurement.getMeasurementCode());
            Meter meter = registryService.getMeter(context.getTenant(), stepDefinition.getStepCode(), stepMeasurement.getMeasurementCode());
            if (meter != null) {
                final MeasurementInstance measurement = meter.measure(context.getTenant(), stepInstance);
                logger.debug("measurement instance found for " + meter.getClass().getName());
                return measurement;
            }
            logger.warn("No meter registered for planned {} measurement of the {} step",
                    stepMeasurement.getMeasurementCode(), stepDefinition.getStepCode());
        }
        return null;
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
}
