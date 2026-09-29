package com.aktimetrix.store.memory;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.store.DefinitionStore;
import com.aktimetrix.core.store.StoreDocuments;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Definitions in memory, by tenant and code.
 */
final class MemoryDefinitionStore implements DefinitionStore {

    private final Map<String, ProcessDefinition> processes = new LinkedHashMap<>();
    private final Map<String, StepDefinition> steps = new LinkedHashMap<>();
    private final Map<String, MeasurementTypeDefinition> measurementTypes = new LinkedHashMap<>();

    private static String key(String tenant, String code) {
        return tenant + '\u0000' + code;
    }

    @Override
    public synchronized ProcessDefinition saveProcess(ProcessDefinition definition) {
        final String key = key(definition.getTenant(), definition.getProcessCode());
        if (definition.getId() == null) {
            final ProcessDefinition existing = processes.get(key);
            definition.setId(existing == null ? MemoryInstanceStores.newId() : existing.getId());
        }
        processes.put(key, StoreDocuments.copy(definition));
        return definition;
    }

    @Override
    public synchronized Optional<ProcessDefinition> findProcess(String tenant, String processCode) {
        return Optional.ofNullable(processes.get(key(tenant, processCode))).map(StoreDocuments::copy);
    }

    @Override
    public synchronized List<ProcessDefinition> findProcesses() {
        return processes.values().stream().map(StoreDocuments::copy).collect(Collectors.toList());
    }

    @Override
    public synchronized List<ProcessDefinition> findConfirmedProcessesStartedBy(String tenant, String eventCode) {
        return processes.values().stream()
                .filter(p -> tenant.equals(p.getTenant()) && "CONFIRMED".equals(p.getStatus())
                        && p.getStartEventCodes() != null && p.getStartEventCodes().contains(eventCode))
                .map(StoreDocuments::copy)
                .collect(Collectors.toList());
    }

    @Override
    public synchronized StepDefinition saveStep(StepDefinition definition) {
        final String key = key(definition.getTenant(), definition.getStepCode());
        if (definition.getId() == null) {
            final StepDefinition existing = steps.get(key);
            definition.setId(existing == null ? MemoryInstanceStores.newId() : existing.getId());
        }
        steps.put(key, StoreDocuments.copy(definition));
        return definition;
    }

    @Override
    public synchronized Optional<StepDefinition> findStep(String tenant, String stepCode) {
        return Optional.ofNullable(steps.get(key(tenant, stepCode))).map(StoreDocuments::copy);
    }

    @Override
    public synchronized List<StepDefinition> findSteps() {
        return steps.values().stream().map(StoreDocuments::copy).collect(Collectors.toList());
    }

    @Override
    public synchronized MeasurementTypeDefinition saveMeasurementType(MeasurementTypeDefinition definition) {
        if (definition.getId() == null) {
            definition.setId(MemoryInstanceStores.newId());
        }
        measurementTypes.put(key(definition.getTenant(), definition.getCode()), StoreDocuments.copy(definition));
        return definition;
    }

    @Override
    public synchronized List<MeasurementTypeDefinition> findMeasurementTypes() {
        return measurementTypes.values().stream().map(StoreDocuments::copy).collect(Collectors.toList());
    }
}
