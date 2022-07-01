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
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Change this to DefaultMeasurementServiceImpl //TODO
 */
@Component
@RequiredArgsConstructor
@Slf4j
@ProcessHandler(processCode = Constants.DEFAULT_PROCESS_CODE, processType = Constants.DEFAULT_PROCESS_TYPE, version = Constants.DEFAULT_VERSION)
public class MeasurementInstanceProcessor implements Processor {
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
//         get default preprocessor
//        final List<PreProcessor> defaultPreProcessors = registryService.getPreProcessor("MI_PUBLISHER");
//        final List<PreProcessor> preProcessors = registryService.getPreProcessor(context.getProcessType());
//        if (!defaultPreProcessors.isEmpty()) {
//            defaultPreProcessors.forEach(preProcessor -> preProcessor.preProcess(context));
//    }
//        preProcessors.forEach(preProcessor -> preProcessor.process(context));
    }

    /**
     * Execute the post processor
     *
     * @param context process context
     */
    private void executePostProcessors(Context context) {
        log.debug("executing post processors");
        // publish the process instance event
//        final List<PostProcessor> postProcessors = registryService.getPostProcessor(context.getProcessType());
//        if (!postProcessors.isEmpty()) {
//            // order the post processor execution by priority TODO
//            postProcessors.forEach(postProcessor -> postProcessor.process(context));
//        }

        // default post processors
        final List<PostProcessor> defaultPostProcessors = registryService.getPostProcessor("MI_PUBLISHER");
        if (!defaultPostProcessors.isEmpty()) {
            defaultPostProcessors.forEach(postProcessor -> postProcessor.process(context));
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

        if (stepDefinition != null && !CollectionUtil.isEmptyOrNull(stepDefinition.getMeasurements())) {
            List<MeasurementInstance> measurementInstances = new ArrayList<>();
            for (StepMeasurement stepMeasurement : stepDefinition.getMeasurements()) {
                stepInstance.setFunctionalCtx(stepDefinition.getFunctionalCtxCode());
                MeasurementInstance measurement = measure(context, stepInstance, stepDefinition, stepMeasurement);
                if (measurement != null) {
                    measurementInstances.add(measurement);
                }
            }
            if (!measurementInstances.isEmpty()) {
                this.measurementInstanceService.saveMeasurementInstances(measurementInstances);
                context.setMeasurementInstances(measurementInstances);
            }
        }
    }

    private MeasurementInstance measure(Context context, StepInstance stepInstance, StepDefinition stepDefinition,
                                        StepMeasurement stepMeasurement) {
        if (MeasurementType.P == stepMeasurement.getType()) {
            logger.info("Step Code: {}, functional Context: {}, Measurement Code: {} ",
                    stepDefinition.getStepCode(), stepInstance.getFunctionalCtx(), stepMeasurement.getMeasurementCode());
            Meter meter = registryService.getMeter(context.getTenant(), stepDefinition.getStepCode(), stepMeasurement.getMeasurementCode());
            if (meter != null) {
                final MeasurementInstance measurement = meter.measure(context.getTenant(), stepInstance);
                // TODO add entity id and entity type
                logger.debug("measurement instance found for " + meter.getClass().getName());
                return measurement;
            }
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
        return this.stepDefinitionService.get(tenant, code, "CONFIRMED");
    }
}
