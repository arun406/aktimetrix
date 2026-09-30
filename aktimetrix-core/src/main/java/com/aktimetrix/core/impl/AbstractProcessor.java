package com.aktimetrix.core.impl;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Context;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.PreProcessor;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.api.MeasurementType;
import com.aktimetrix.core.meter.api.ProcessMeter;
import com.aktimetrix.core.model.MeasurementInstance;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.model.MeasurementDefinition;
import com.aktimetrix.core.service.AktimetrixMetrics;
import com.aktimetrix.core.service.MeasurementInstancePublisherService;
import com.aktimetrix.core.service.MeasurementInstanceService;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.service.StepInstanceService;
import com.aktimetrix.core.service.StepPlanner;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Base class for process handlers. Creates the process instance and its step instances for a business entity,
 * computes the planned measurements of the process and of each step with the registered meters, and runs the pre-
 * and post-processors of the process type.
 * <p>
 * Subclasses decide which metadata to keep on the process instance and on its step instances.
 *
 * @author arun kumar kandakatla
 */
public abstract class AbstractProcessor implements Processor {
    final private static Logger logger = LoggerFactory.getLogger(AbstractProcessor.class);

    @Autowired
    private StepInstanceService stepInstanceService;
    @Autowired
    private ProcessInstanceService processInstanceService;
    @Autowired
    private RegistryService registryService;
    @Autowired
    private StepPlanner stepPlanner;
    @Autowired
    private AktimetrixMetrics metrics;
    @Autowired
    private MeasurementInstanceService measurementInstanceService;
    @Autowired
    private MeasurementInstancePublisherService measurementInstancePublisher;

    /**
     * @param context process context
     */
    @Override
    public void process(Context context) {
        executePreProcessors(context);
        doProcess(context);
        measureProcess(context);
        planSteps(context);
        executePostProcessors(context);
    }

    private void executePreProcessors(Context context) {
        logger.debug("executing the preprocessors");
        final List<PreProcessor> preProcessors = registryService.getPreProcessor(context.getProcessType());
        preProcessors.forEach(preProcessor -> preProcessor.process(context));
    }

    public void doProcess(Context context) {
        ProcessInstance processInstance = getProcessInstance(context);
        if (processInstance.getId() != null) {
            // replayed start event: keep the existing steps and publish nothing new for them
            logger.info("Process instance {} already exists", processInstance.getId());
            processInstance.setSteps(stepInstanceService.getStepInstancesByProcessInstanceId(context.getTenant(),
                    processInstance.getId()));
            context.setStepInstances(new ArrayList<>());
            context.setProcessInstance(processInstance);
            return;
        }
        try {
            final List<StepDefinition> stepDefinitions = getStepDefinitions(context);
            logger.info("Saving Process Instance");
            saveProcessInstance(processInstance);
            metrics.processStarted(processInstance);
            logger.info("Saving the step instances..");
            final List<StepInstance> stepInstances = saveStepInstances(context.getTenant(), stepDefinitions,
                    processInstance.getId(), getStepMetadata(context));
            processInstance.setSteps(Objects.requireNonNullElseGet(stepInstances, ArrayList::new));

            context.setStepInstances(stepInstances);
            context.setProcessInstance(processInstance);
        } catch (DefinitionNotFoundException e) {
            logger.error("step definitions are not available for this process", e);
        }
    }

    /**
     * Computes the planned measurements of a newly created process instance with its process-level meters.
     */
    private void measureProcess(Context context) {
        final ProcessDefinition definition = (ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION);
        // a replayed start event leaves no new steps, and its process was measured when it was created
        if (context.getStepInstances().isEmpty() || definition == null || definition.getMeasurements() == null) {
            return;
        }
        final ProcessInstance processInstance = context.getProcessInstance();
        final List<MeasurementInstance> measurements = new ArrayList<>();
        for (MeasurementDefinition measurement : definition.getMeasurements()) {
            if (MeasurementType.P != measurement.getType()) {
                continue;
            }
            final ProcessMeter meter = registryService.getProcessMeter(context.getTenant(),
                    definition.getProcessCode(), measurement.getMeasurementCode());
            if (meter != null) {
                final MeasurementInstance planned = meter.measure(context.getTenant(), processInstance);
                if (planned != null) {
                    measurements.add(planned);
                }
            } else if (measurement.getValue() != null) {
                measurements.add(new MeasurementInstance(context.getTenant(), measurement.getMeasurementCode(),
                        measurement.getValue(), measurement.getUnit(), processInstance.getId(), null, null,
                        Constants.PLAN_MEASUREMENT_TYPE, null, ZonedDateTime.now()));
            } else {
                logger.warn("No process-level meter, and no planned value, for {} of the {} process",
                        measurement.getMeasurementCode(), definition.getProcessCode());
            }
        }
        if (measurements.isEmpty()) {
            return;
        }
        applyPlannedTime(processInstance, definition, measurements);
        measurementInstanceService.saveMeasurementInstances(measurements);
        final DefaultContext measurementContext = new DefaultContext();
        measurementContext.setTenant(context.getTenant());
        measurementContext.setMeasurementInstances(measurements);
        measurementInstancePublisher.postProcess(measurementContext);
    }

    /**
     * A planned TIME of the process, from a meter (a rule, e.g. priority customers within 1 day) or a fixed value, is
     * its deadline; it replaces one computed from {@code plannedWithin}.
     */
    private void applyPlannedTime(ProcessInstance processInstance, ProcessDefinition definition,
                                  List<MeasurementInstance> measurements) {
        for (MeasurementInstance measurement : measurements) {
            if (Constants.MEASUREMENT_CODE_TIME.equals(measurement.getCode()) && measurement.getValue() != null) {
                try {
                    processInstance.setPlannedAt(LocalDateTime.parse(measurement.getValue()));
                } catch (DateTimeParseException e) {
                    logger.warn("Planned TIME of the {} process is not an ISO date-time: {}",
                            definition.getProcessCode(), measurement.getValue());
                    return;
                }
                processInstance.setLateAfter(processInstance.getPlannedAt().plus(definition.toleranceDuration()));
                processInstanceService.saveProcessInstance(processInstance);
                return;
            }
        }
    }

    /**
     * Plans the newly created steps: first with the registered meters, then from the durations in their
     * definitions, and finally sets every planned step's deadline.
     */
    private void planSteps(Context context) {
        if (context.getStepInstances().isEmpty()) {
            return;
        }
        runMeters(context);
        final Map<String, StepDefinition> definitions = new HashMap<>();
        try {
            getStepDefinitions(context).forEach(definition -> definitions.put(definition.getStepCode(), definition));
        } catch (DefinitionNotFoundException e) {
            return;
        }
        stepPlanner.planNewSteps(context.getStepInstances(), definitions,
                        context.getProcessInstance().getStartedAt())
                .forEach(stepInstanceService::save);
    }

    private void runMeters(Context context) {
        final Processor meterProcessor;
        try {
            meterProcessor = registryService.getProcessHandler(Constants.METER_PROCESSOR);
        } catch (ProcessHandlerNotFoundException e) {
            logger.warn("No meter processor registered; planned measurements are not computed");
            return;
        }
        for (StepInstance step : context.getStepInstances()) {
            DefaultContext stepContext = new DefaultContext();
            stepContext.setTenant(context.getTenant());
            stepContext.setProcessType(Constants.METER_PROCESSOR);
            stepContext.setProperty(Constants.PROCESS_DEFINITION, context.getProperty(Constants.PROCESS_DEFINITION));
            stepContext.setStepInstances(new ArrayList<>(List.of(step)));
            meterProcessor.process(stepContext);
        }
    }

    public List<StepDefinition> getStepDefinitions(Context context) throws DefinitionNotFoundException {
        return new DefaultStepDefinitionProvider((ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION)).getDefinitions();
    }

    /**
     * Metadata to store on each step instance, available to meters as {@code step.getMetadata()}.
     */
    protected abstract Map<String, Object> getStepMetadata(Context context);

    /**
     * Metadata to store on the process instance.
     */
    protected abstract Map<String, Object> getProcessMetadata(Context context);

    private void executePostProcessors(Context context) {
        logger.debug("executing post processors");
        final List<PostProcessor> postProcessors = registryService.getPostProcessor(context.getProcessType());
        postProcessors.forEach(postProcessor -> postProcessor.process(context));
    }

    private ProcessInstance saveProcessInstance(ProcessInstance processInstance) {
        return this.processInstanceService.saveProcessInstance(processInstance);
    }

    private List<StepInstance> saveStepInstances(String tenant, List<StepDefinition> stepDefinitions,
                                                 String processInstanceId, Map<String, Object> metadata) {
        return this.stepInstanceService
                .save(tenant, stepDefinitions, metadata, processInstanceId);
    }

    private ProcessInstance getProcessInstance(Context context) {
        ProcessDefinition definition = (ProcessDefinition) context.getProperty(Constants.PROCESS_DEFINITION);
        String entityId = (String) context.getProperty(Constants.ENTITY_ID);
        // check process instance already exists for this entity type, entity id, process code combination
        ProcessInstance processInstance = processInstanceService.getProcessInstance(context.getTenant(),
                definition.getProcessCode(), definition.getEntityType(), entityId);
        if (processInstance == null) {
            processInstance = new ProcessInstance(definition);
            processInstance.setMetadata(getProcessMetadata(context));
            processInstance.setEntityId(entityId);
            processInstance.setStartedAt((LocalDateTime) context.getProperty(Constants.OCCURRED_AT));
            if (definition.plannedWithinDuration() != null && processInstance.getStartedAt() != null) {
                processInstance.setPlannedAt(processInstance.getStartedAt().plus(definition.plannedWithinDuration()));
                processInstance.setLateAfter(processInstance.getPlannedAt().plus(definition.toleranceDuration()));
            }
        }
        return processInstance;
    }
}
