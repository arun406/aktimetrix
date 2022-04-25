package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.EventType;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.PreProcessor;
import com.aktimetrix.core.api.ProcessType;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.exception.EventHandlerNotFoundException;
import com.aktimetrix.core.exception.MultipleEventHandlerFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.impl.RegistryEntry;
import com.aktimetrix.core.meter.api.MeasurementProcessor;
import com.aktimetrix.core.meter.api.Meter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class RegistryService {
    final private Logger logger = LoggerFactory.getLogger(RegistryService.class);

    @Autowired
    private Registry registry;

    public List<PreProcessor> getPreProcessor(ProcessType processType) {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_PRE_PROCESSOR_SERVICE)
                && re.attribute(Constants.ATT_PRE_PROCESSOR_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate2 = re -> (re.attribute(Constants.ATT_PRE_PROCESSOR_PROCESS_TYPE) == processType);

        final List<Object> preProcessors = this.registry.lookupAll(predicate1.and(predicate2));
        logger.debug("Applicable pre processors {}", preProcessors);

        if (preProcessors != null && !preProcessors.isEmpty()) {
            return preProcessors.stream().map(m -> (PreProcessor) m).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    public List<PostProcessor> getPostProcessor(ProcessType processType) {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_POST_PROCESSOR_SERVICE)
                && re.attribute(Constants.ATT_POST_PROCESSOR_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate2 = re -> re.hasAttribute(Constants.ATT_POST_PROCESSOR_PROCESS_TYPE) &&
                (processType == ProcessType.valueOf((String) re.attribute(Constants.ATT_POST_PROCESSOR_PROCESS_TYPE)));

        final List<Object> postProcessors = this.registry.lookupAll(predicate1.and(predicate2));
        logger.debug("Applicable post processors {}", postProcessors);

        if (postProcessors != null && !postProcessors.isEmpty()) {
            return postProcessors.stream().map(m -> (PostProcessor) m).collect(Collectors.toList());
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
    public Processor getProcessHandler(ProcessType processType) throws ProcessHandlerNotFoundException {
        final List<Object> handlers = this.registry
                .lookupAll(registryEntry -> registryEntry.hasAttribute(Constants.ATT_PROCESS_HANDLER_SERVICE) &&
                        registryEntry.attribute(Constants.ATT_PROCESS_HANDLER_SERVICE).equals(Constants.VAL_YES) &&
                        (ProcessType.valueOf((String) registryEntry.attribute(Constants.ATT_PROCESS_TYPE)) == processType)
                );
        logger.debug("applicable handlers {}", handlers);
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
    public EventHandler getEventHandler(EventType eventType) throws EventHandlerNotFoundException, MultipleEventHandlerFoundException {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_EVENT_HANDLER_SERVICE);
        Predicate<RegistryEntry> predicate2 = re -> re.attribute(Constants.ATT_EVENT_HANDLER_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate3 = re -> (EventType.valueOf((String) re.attribute(Constants.ATT_EVENT_TYPE)) == eventType);

        final List<Object> eventHandlers = this.registry.lookupAll(predicate1.and(predicate2).and(predicate3));
        logger.debug("Applicable eventHandlers {}", eventHandlers);
        if (eventHandlers == null || eventHandlers.isEmpty()) {
            throw new EventHandlerNotFoundException(String.format("event handlers not found for %s", eventType));
        }
        if (eventHandlers.size() > 1) {
            throw new MultipleEventHandlerFoundException();
        }
        EventHandler eventHandler = null;
        for (Object m : eventHandlers) {
            eventHandler = (EventHandler) m;
        }
        return eventHandler;
    }


    /**
     * Returns the process Handler Based on Process Type
     *
     * @param processType event type
     * @return Event Handler Object
     * @throws ProcessHandlerNotFoundException
     */
    public MeasurementProcessor getMeasurementProcessHandler(ProcessType processType) throws ProcessHandlerNotFoundException {
        final List<Object> handlers = this.registry
                .lookupAll(registryEntry -> registryEntry.hasAttribute(com.aktimetrix.core.api.Constants.ATT_PROCESS_HANDLER_SERVICE) &&
                        registryEntry.attribute(com.aktimetrix.core.api.Constants.ATT_PROCESS_HANDLER_SERVICE).equals(com.aktimetrix.core.api.Constants.VAL_YES) &&
                        (ProcessType.valueOf((String) registryEntry.attribute(com.aktimetrix.core.api.Constants.ATT_PROCESS_TYPE)) == processType)
                );
        logger.debug("applicable handlers {}", handlers);
        if (handlers == null || handlers.isEmpty()) {
            throw new ProcessHandlerNotFoundException(String.format("process handlers not found for %s", processType));
        }

        MeasurementProcessor processHandler = null;
        for (Object m : handlers) {
            processHandler = (MeasurementProcessor) m;
        }
        return processHandler;
    }

    /**
     * Return applicable meter instance
     *
     * @param tenant          tenant parameter
     * @param stepCode        step code
     * @param measurementCode measurement code
     */
    public Meter getMeter(String tenant, String stepCode, String measurementCode) {
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
}
