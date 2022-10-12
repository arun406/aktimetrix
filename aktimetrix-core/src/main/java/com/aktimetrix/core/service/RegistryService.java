package com.aktimetrix.core.service;

import com.aktimetrix.core.api.*;
import com.aktimetrix.core.exception.*;
import com.aktimetrix.core.impl.RegistryEntry;
import com.aktimetrix.core.meter.api.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class RegistryService {
    final private Logger logger = LoggerFactory.getLogger(RegistryService.class);

    @Autowired
    private Registry registry;

    public List<PreProcessor> getPreProcessor(String code) {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_PRE_PROCESSOR_SERVICE)
                && re.attribute(Constants.ATT_PRE_PROCESSOR_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate2 = re -> re.hasAttribute(Constants.ATT_PRE_PROCESSOR_CODE) && (re.attribute(Constants.ATT_PRE_PROCESSOR_CODE).equals(code));
        final List<Object> preProcessors = this.registry.lookupAll(predicate1.and(predicate2));
        logger.debug("Applicable pre processors {}", preProcessors);

        if (preProcessors != null && !preProcessors.isEmpty()) {
            return preProcessors.stream().map(m -> (PreProcessor) m).collect(Collectors.toList()); //TODO
        }
        return new ArrayList<>();
    }

    public List<PreProcessor> getPreProcessor(String processType, String processCode) {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_PRE_PROCESSOR_SERVICE)
                && re.attribute(Constants.ATT_PRE_PROCESSOR_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate2 = re -> (re.attribute(Constants.ATT_PRE_PROCESSOR_PROCESS_TYPE).equals(processType));
        Predicate<RegistryEntry> predicate3 = re -> re.hasAttribute(Constants.ATT_PRE_PROCESSOR_PROCESS_CODE) &&
                (processCode.equals(re.attribute(Constants.ATT_PRE_PROCESSOR_PROCESS_CODE)));
        final List<Object> preProcessors = this.registry.lookupAll(predicate1.and(predicate2).and(predicate3));
        logger.debug("Applicable pre processors {}", preProcessors);

        if (preProcessors != null && !preProcessors.isEmpty()) {
            return preProcessors.stream().map(m -> (PreProcessor) m).collect(Collectors.toList()); //TODO
        }
        return new ArrayList<>();
    }

    public PostProcessor getPostProcessor(String code) throws PostProcessorNotFoundException, MultiplePostProcessFoundException {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_POST_PROCESSOR_SERVICE)
                && re.attribute(Constants.ATT_POST_PROCESSOR_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate2 = re -> re.hasAttribute(Constants.ATT_POST_PROCESSOR_CODE) &&
                (code.equals(re.attribute(Constants.ATT_POST_PROCESSOR_CODE)));

        final List<Object> postProcessors = this.registry.lookupAll(predicate1.and(predicate2));
        logger.debug("Applicable post processors {}", postProcessors);


        if (postProcessors == null || postProcessors.isEmpty()) {
            throw new PostProcessorNotFoundException(String.format("Post Processor handlers not found with code %s", code));
        }
        if (postProcessors.size() > 1) {
            throw new MultiplePostProcessFoundException(String.format("Multiple post processors exists for the same code %s", code));
        }
        PostProcessor postProcessor = null;
        for (Object m : postProcessors) {
            postProcessor = (PostProcessor) m;
        }
        return postProcessor;
    }

    public List<PostProcessor> getPostProcessor(String processType, String processCode) {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_POST_PROCESSOR_SERVICE)
                && re.attribute(Constants.ATT_POST_PROCESSOR_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate2 = re -> re.hasAttribute(Constants.ATT_POST_PROCESSOR_PROCESS_TYPE) &&
                (processType.equals(re.attribute(Constants.ATT_POST_PROCESSOR_PROCESS_TYPE)));
        Predicate<RegistryEntry> predicate3 = re -> re.hasAttribute(Constants.ATT_POST_PROCESSOR_PROCESS_CODE) &&
                (processCode.equals(re.attribute(Constants.ATT_POST_PROCESSOR_PROCESS_CODE)));

        final List<Object> postProcessors = this.registry.lookupAll(predicate1.and(predicate2).and(predicate3));
        logger.debug("Applicable post processors {}", postProcessors);

        if (postProcessors != null && !postProcessors.isEmpty()) {
            return postProcessors.stream().map(m -> (PostProcessor) m).collect(Collectors.toList()); //TODO
        }
        return new ArrayList<>();
    }

    /**
     * Returns the process Handler Based on Process Type
     *
     * @param processType event type
     * @return Event Handler Object
     * @throws ProcessHandlerNotFoundException
     */
    public Processor getProcessors(String processType, String processCode) throws ProcessHandlerNotFoundException,
            MultipleProcessHandlersFoundException {

        final List<Object> handlers = this.registry
                .lookupAll(registryEntry -> {
                            final String processCodes = (String) registryEntry.attribute(Constants.ATT_PROCESS_CODE);
                            final String[] split = processCodes.split(",");
                            return registryEntry.hasAttribute(Constants.ATT_PROCESS_HANDLER_SERVICE) &&
                                    registryEntry.attribute(Constants.ATT_PROCESS_HANDLER_SERVICE).equals(Constants.VAL_YES) &&
                                    processType.equals(registryEntry.attribute(Constants.ATT_PROCESS_TYPE)) &&
                                    Arrays.asList(split).contains(processCode);
                        }
                );
        logger.debug("applicable handlers {}", handlers);
        if (handlers == null || handlers.isEmpty()) {
            throw new ProcessHandlerNotFoundException(String.format("event handlers not found for %s, %s", processType, processCode));
        }
        if (handlers.size() > 1) {
            throw new MultipleProcessHandlersFoundException(String.format("Multiple event handlers exists for the same process type %s, and process code %s", processType, processCode));
        }
        Processor processHandler = null;
        for (Object m : handlers) {
            processHandler = (Processor) m;
        }
        return processHandler;
    }

    /**
     * Returns the Event Handler Based on Event Type
     *
     * @param eventType event type
     * @return Event Handler Object
     * @throws EventHandlerNotFoundException
     */
    public EventHandler getEventHandler(String eventType, String eventCode) throws EventHandlerNotFoundException, MultipleEventHandlersFoundException {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_EVENT_HANDLER_SERVICE);
        Predicate<RegistryEntry> predicate2 = re -> re.attribute(Constants.ATT_EVENT_HANDLER_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate3 = re -> re.attribute(Constants.ATT_EVENT_TYPE).equals(eventType);
        Predicate<RegistryEntry> predicate4 = re -> re.attribute(Constants.ATT_EVENT_CODE).equals(eventCode);

        final List<Object> eventHandlers = this.registry.lookupAll(predicate1.and(predicate2).and(predicate3).and(predicate4));
        logger.debug("Applicable eventHandlers {}", eventHandlers);
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            throw new EventHandlerNotFoundException(String.format("event handlers not found for %s", eventType));
        }
        if (eventHandlers.size() > 1) {
            throw new MultipleEventHandlersFoundException();
        }
        EventHandler eventHandler = null;
        for (Object m : eventHandlers) {
            eventHandler = (EventHandler) m;
        }
        return eventHandler;
    }


    /**
     * Return applicable plan meter instance
     *
     * @param stepCode        step code
     * @param measurementCode measurement code
     */
    public Meter getMeter(String stepCode, String measurementCode) {
        final List<Object> meters = this.registry.lookupAll(registryEntry ->
                registryEntry.hasAttribute(Constants.ATT_METER_SERVICE) &&
                        registryEntry.attribute(Constants.ATT_METER_SERVICE).equals(Constants.VAL_YES) &&
                        (registryEntry.attribute(Constants.ATT_CODE).equals(measurementCode)
                                && registryEntry.attribute(Constants.ATT_STEP_CODE).equals(stepCode)
                                && registryEntry.attribute(Constants.ATT_MEASUREMENT_TYPE).equals("P")
                        )
        );
        Meter meter = null;
        for (Object m : meters) {
            meter = (Meter) m;
        }
        return meter;
    }

    /**
     * Return applicable plan meter instance
     *
     * @param stepCode        step code
     * @param measurementCode measurement code
     * @param type            measurement type
     */
    public Meter getMeter(String stepCode, String measurementCode, String type) {
        final List<Object> meters = this.registry.lookupAll(registryEntry ->
                registryEntry.hasAttribute(Constants.ATT_METER_SERVICE) &&
                        registryEntry.attribute(Constants.ATT_METER_SERVICE).equals(Constants.VAL_YES) &&
                        (registryEntry.attribute(Constants.ATT_CODE).equals(measurementCode)
                                && registryEntry.attribute(Constants.ATT_STEP_CODE).equals(stepCode)
                                && registryEntry.attribute(Constants.ATT_MEASUREMENT_TYPE).equals(type)
                        )
        );
        Meter meter = null;
        for (Object m : meters) {
            meter = (Meter) m;
        }
        return meter;
    }
}
