package com.aktimetrix.core.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.EventHandler;
import com.aktimetrix.core.api.PostProcessor;
import com.aktimetrix.core.api.PreProcessor;
import com.aktimetrix.core.api.Processor;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.exception.EventHandlerNotFoundException;
import com.aktimetrix.core.exception.MultipleEventHandlerFoundException;
import com.aktimetrix.core.exception.ProcessHandlerNotFoundException;
import com.aktimetrix.core.exception.UnknownNameException;
import com.aktimetrix.core.impl.RegistryEntry;
import com.aktimetrix.core.meter.api.Meter;
import com.aktimetrix.core.meter.api.ProcessMeter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Component
public class RegistryService {
    final private Logger logger = LoggerFactory.getLogger(RegistryService.class);

    @Autowired
    private Registry registry;

    /**
     * Pre-processors registered for the process type or for all processes ({@code "*"}), lowest priority first.
     */
    public List<PreProcessor> getPreProcessor(String processType) {
        return getPreProcessor(processType, true);
    }

    /**
     * Pre-processors registered for the process type, lowest priority first.
     *
     * @param includeAllProcesses whether to include pre-processors registered for all processes ({@code "*"})
     */
    public List<PreProcessor> getPreProcessor(String processType, boolean includeAllProcesses) {
        return lookupByPriority(Constants.ATT_PRE_PROCESSOR_SERVICE, Constants.ATT_PRE_PROCESSOR_PROCESS_TYPE,
                processType, includeAllProcesses, PreProcessor.class);
    }

    /**
     * Post-processors registered for the process type or for all processes ({@code "*"}), lowest priority first.
     */
    public List<PostProcessor> getPostProcessor(String processType) {
        return getPostProcessor(processType, true);
    }

    /**
     * Post-processors registered for the process type, lowest priority first.
     *
     * @param includeAllProcesses whether to include post-processors registered for all processes ({@code "*"})
     */
    public List<PostProcessor> getPostProcessor(String processType, boolean includeAllProcesses) {
        return lookupByPriority(Constants.ATT_POST_PROCESSOR_SERVICE, Constants.ATT_POST_PROCESSOR_PROCESS_TYPE,
                processType, includeAllProcesses, PostProcessor.class);
    }

    private <T> List<T> lookupByPriority(String serviceAttribute, String processTypeAttribute, String processType,
                                         boolean includeAllProcesses, Class<T> type) {
        Predicate<RegistryEntry> isService = re -> Constants.VAL_YES.equals(re.attribute(serviceAttribute));
        Predicate<RegistryEntry> matchesType = re -> {
            Object registeredType = re.attribute(processTypeAttribute);
            return Objects.equals(registeredType, processType)
                    || (includeAllProcesses && Constants.ALL_PROCESS_TYPES.equals(registeredType));
        };
        Comparator<RegistryEntry> byPriority = Comparator.comparingInt(re -> priority(re.attribute(Constants.ATT_PRE_PROCESSOR_PRIORITY)));
        final List<Object> found;
        try {
            found = this.registry.lookupAll(isService.and(matchesType), byPriority);
        } catch (UnknownNameException e) {
            return new ArrayList<>();
        }
        logger.debug("Applicable {}s for {}: {}", type.getSimpleName(), processType, found);
        return found.stream().map(type::cast).collect(Collectors.toList());
    }

    private static int priority(Object value) {
        try {
            return value == null ? 1 : Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 1;
        }
    }

    /**
     * Returns the process Handler Based on Process Type
     *
     * @param processType event type
     * @return Event Handler Object
     * @throws ProcessHandlerNotFoundException
     */
    public Processor getProcessHandler(String processType) throws ProcessHandlerNotFoundException {
        final List<Object> handlers = this.registry
                .lookupAll(registryEntry -> registryEntry.hasAttribute(Constants.ATT_PROCESS_HANDLER_SERVICE) &&
                        registryEntry.attribute(Constants.ATT_PROCESS_HANDLER_SERVICE).equals(Constants.VAL_YES) &&
                        processType.equals(registryEntry.attribute(Constants.ATT_PROCESS_TYPE))
                );
        logger.debug("applicable handlers {}", handlers);
        if (handlers.isEmpty()) {
            throw new ProcessHandlerNotFoundException("No @ProcessHandler registered for " + processType);
        }
        return (Processor) handlers.get(handlers.size() - 1);
    }

    /**
     * Returns the Event Handler Based on Event Type
     *
     * @param eventType event type
     * @return Event Handler Object
     * @throws EventHandlerNotFoundException
     */
    public EventHandler getEventHandler(String eventType) throws EventHandlerNotFoundException, MultipleEventHandlerFoundException {
        Predicate<RegistryEntry> predicate1 = re -> re.hasAttribute(Constants.ATT_EVENT_HANDLER_SERVICE);
        Predicate<RegistryEntry> predicate2 = re -> re.attribute(Constants.ATT_EVENT_HANDLER_SERVICE).equals(Constants.VAL_YES);
        Predicate<RegistryEntry> predicate3 = re -> re.attribute(Constants.ATT_EVENT_TYPE).equals(eventType);

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
     * The {@code @Measurement} meter of the step and measurement code, or {@code null}. Planning rules of the DSL
     * are not returned: see {@link #planMeter}.
     *
     * @param tenant          tenant parameter
     * @param stepCode        step code
     * @param measurementCode measurement code
     */
    public Meter getMeter(String tenant, String stepCode, String measurementCode) {
        return lookupMeter(Constants.ATT_STEP_CODE, stepCode, measurementCode, Meter.class, null, null, false);
    }

    /**
     * The {@code @Measurement} process-level meter of the process and measurement code, or {@code null}. Planning
     * rules of the DSL are not returned: see {@link #processPlanMeter}.
     *
     * @param tenant          tenant parameter
     * @param processCode     process code
     * @param measurementCode measurement code
     */
    public ProcessMeter getProcessMeter(String tenant, String processCode, String measurementCode) {
        return lookupMeter(Constants.ATT_PROCESS_CODE, processCode, measurementCode, ProcessMeter.class, null, null,
                false);
    }

    /**
     * The meter that plans a measurement of a step of a process: the most specific of the planning rules and meters
     * that apply, a rule limited to the tenant's process before one limited to the tenant, before a meter for any.
     */
    public Meter planMeter(String tenant, String processCode, String stepCode, String measurementCode) {
        return lookupMeter(Constants.ATT_STEP_CODE, stepCode, measurementCode, Meter.class, tenant, processCode, true);
    }

    /**
     * The meter that plans a measurement of a process: a planning rule of the tenant before a meter for any.
     */
    public ProcessMeter processPlanMeter(String tenant, String processCode, String measurementCode) {
        return lookupMeter(Constants.ATT_PROCESS_CODE, processCode, measurementCode, ProcessMeter.class, tenant, null,
                true);
    }

    private <T> T lookupMeter(String levelAttribute, String levelCode, String measurementCode, Class<T> type,
                              String tenant, String processCode, boolean withRules) {
        final List<RegistryEntry> entries = this.registry.lookupAllEntries(registryEntry ->
                registryEntry.hasAttribute(Constants.ATT_METER_SERVICE) &&
                        registryEntry.attribute(Constants.ATT_METER_SERVICE).equals(Constants.VAL_YES) &&
                        Objects.equals(registryEntry.attribute(Constants.ATT_CODE), measurementCode) &&
                        Objects.equals(registryEntry.attribute(levelAttribute), levelCode)
        );
        T meter = null;
        int best = -1;
        for (RegistryEntry entry : entries) {
            final Object ruleTenant = entry.attribute(Constants.ATT_RULE_TENANT);
            final Object ruleProcess = entry.attribute(Constants.ATT_RULE_PROCESS);
            final boolean rule = ruleTenant != null || ruleProcess != null;
            if (rule && (!withRules || (ruleTenant != null && !ruleTenant.equals(tenant))
                    || (ruleProcess != null && !ruleProcess.equals(processCode)))) {
                continue;
            }
            final Object m = instance(entry);
            final int specificity = (ruleProcess != null ? 2 : 0) + (ruleTenant != null ? 1 : 0);
            if (type.isInstance(m) && specificity >= best) {
                meter = type.cast(m);
                best = specificity;
            }
        }
        return meter;
    }

    private static Object instance(RegistryEntry entry) {
        try {
            return entry.getInstance();
        } catch (IllegalAccessException | InstantiationException e) {
            throw new IllegalStateException("Cannot create the meter " + entry, e);
        }
    }
}
