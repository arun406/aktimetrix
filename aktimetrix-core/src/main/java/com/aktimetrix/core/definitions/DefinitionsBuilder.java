package com.aktimetrix.core.definitions;

import com.aktimetrix.core.referencedata.model.StepDefinition;

import java.util.function.Consumer;

/**
 * Builds the {@link Definitions} of one tenant: its shared steps and its processes.
 */
public final class DefinitionsBuilder {

    private final Definitions definitions = new Definitions();

    DefinitionsBuilder(String tenant) {
        definitions.setTenant(tenant);
    }

    /**
     * A step shared by the tenant's processes, which list it by its code and may adapt it.
     */
    public DefinitionsBuilder step(String stepCode, Consumer<StepBuilder> step) {
        final StepBuilder builder = new StepBuilder(stepCode, definitions.getTenant(), null, definitions.getRules());
        step.accept(builder);
        final StepDefinition definition = builder.build();
        definition.setTenant(definitions.getTenant());
        if (definition.getStatus() == null) {
            definition.setStatus(ProcessBuilder.CONFIRMED);
        }
        definitions.getSteps().add(definition);
        return this;
    }

    /**
     * A process of the tenant.
     */
    public DefinitionsBuilder process(String processCode, Consumer<ProcessBuilder> process) {
        final ProcessBuilder builder = new ProcessBuilder(processCode, definitions.getTenant(), definitions.getRules());
        process.accept(builder);
        definitions.getProcesses().add(builder.build());
        return this;
    }

    public Definitions build() {
        return definitions;
    }
}
