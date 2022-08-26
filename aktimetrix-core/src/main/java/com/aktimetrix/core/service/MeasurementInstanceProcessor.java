package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.exception.MultiplePostProcessFoundException;
import com.aktimetrix.core.exception.PostProcessorNotFoundException;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.model.StepMeasurement;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.stereotypes.Processor;
import com.aktimetrix.core.util.CollectionUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Change this to DefaultMeasurementServiceImpl
 */
@Slf4j
@RequiredArgsConstructor
@Component
@Processor(processCode = Constants.MEASUREMENT_INSTANCE_CREATE, processType = Constants.MEASUREMENT_INSTANCE_TYPE)
public class MeasurementInstanceProcessor implements com.aktimetrix.core.api.Processor {
    final private Logger logger = LoggerFactory.getLogger(MeasurementInstanceProcessor.class);

    final private StepDefinitionService stepDefinitionService;
    final private MeasurementInstanceService measurementInstanceService;
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

    protected void executePreProcessors(Context context) {
        // get the preprocessors from registry
        log.debug("executing the pre processors");
    }

    /**
     * Execute the post processor
     *
     * @param context process context
     */
    private void executePostProcessors(Context context) {
        log.debug("executing post processors");
        try {
            final PostProcessor defaultPostProcessors = registryService.getPostProcessor("MI_PUBLISHER");
            defaultPostProcessors.process(context);
        } catch (PostProcessorNotFoundException | MultiplePostProcessFoundException e) {
            log.error(e.getMessage(), e);
        }
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

        if (stepDefinition == null || CollectionUtil.isEmptyOrNull(stepDefinition.getMeasurements())) {
            return;
        }
        List<MeasurementInstance> measurementInstances = new ArrayList<>();
        for (StepMeasurement stepMeasurement : stepDefinition.getMeasurements()) {
            stepInstance.setFunctionalCtx(stepDefinition.getFunctionalCtxCode());
            final String measurementType = Constants.STEP_COMPLETED.equals(stepInstance.getStatus()) ? "P" : "A";
            final Meter meter = registryService.getMeter(stepDefinition.getStepCode(),
                    stepMeasurement.getMeasurementCode(), measurementType);
            if (meter == null) {
                continue;
            }
            logger.info("Step Code: {}, functional Context: {}, Measurement Code: {} ",
                    stepInstance.getStepCode(), stepInstance.getFunctionalCtx(), stepMeasurement.getMeasurementCode());
            MeasurementInstance measurement = measure(context, stepInstance, meter);
            if (measurement != null) {
                measurementInstances.add(measurement);
            }
        }
        if (!measurementInstances.isEmpty()) {
            this.measurementInstanceService.saveMeasurementInstances(measurementInstances);
            context.setMeasurementInstances(measurementInstances);
        }
    }

    /**
     * Take Measurements
     *
     * @param context
     * @param stepInstance
     * @param meter
     * @return
     */
    private MeasurementInstance measure(Context context, StepInstance stepInstance, Meter meter) {
        Objects.requireNonNull(meter);
        return meter.measure(context.getTenant(), stepInstance);
    }

    /**
     * Returns Step Definition
     *
     * @param tenant tenant
     * @param code   step code
     * @return step definition
     */
    private StepDefinition getStepDefinition(String tenant, String code) {
        return this.stepDefinitionService.get(tenant, code, "CONFIRMED");
    }
}
