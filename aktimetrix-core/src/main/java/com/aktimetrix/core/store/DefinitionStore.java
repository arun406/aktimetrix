package com.aktimetrix.core.store;

import com.aktimetrix.core.referencedata.model.MeasurementTypeDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;

import java.util.List;
import java.util.Optional;

/**
 * Stores process, step and measurement type definitions. Saving a definition replaces the one with the same tenant
 * and code, if any.
 */
public interface DefinitionStore {

    ProcessDefinition saveProcess(ProcessDefinition definition);

    Optional<ProcessDefinition> findProcess(String tenant, String processCode);

    List<ProcessDefinition> findProcesses();

    /**
     * The {@code CONFIRMED} process definitions of the tenant that list the event code among their start events.
     */
    List<ProcessDefinition> findConfirmedProcessesStartedBy(String tenant, String eventCode);

    StepDefinition saveStep(StepDefinition definition);

    Optional<StepDefinition> findStep(String tenant, String stepCode);

    List<StepDefinition> findSteps();

    MeasurementTypeDefinition saveMeasurementType(MeasurementTypeDefinition definition);

    List<MeasurementTypeDefinition> findMeasurementTypes();
}
