package com.aktimetrix.core.definitions;

import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * A set of process and step definitions for one tenant, with the planning rules they need. It is what a YAML
 * definition file holds, and what the Java DSL builds:
 *
 * <pre>{@code
 * @Bean
 * Definitions orderDelivery() {
 *     return Definitions.tenant("SHOP")
 *             .process("ORDER_DELIVERY", order -> order
 *                     .entityType("order")
 *                     .startsOn("ORDER_CREATED")
 *                     .cancelledOn("ORDER_CANCELLED")
 *                     .step("CONFIRM", step -> step.on("ORDER_CONFIRMED").within("PT5M"))
 *                     .step("PAY", step -> step.on("PAYMENT_CONFIRMED").after("CONFIRM").within("PT15M")
 *                             .tolerance("PT5M"))
 *                     .step("DELIVERED", step -> step.on("DELIVERED")
 *                             .planTime(delivered -> metadataTime(delivered, "createdAt").plusHours(4))))
 *             .build();
 * }
 * }</pre>
 * (with {@code import static com.aktimetrix.core.definitions.Planning.metadataTime}.)
 * <p>
 * Aktimetrix loads every {@code Definitions} bean at startup, like the definition files, and registers its planning
 * rules as meters. The definitions are saved by tenant and code: a change makes a new revision, and running instances
 * keep the revision they started with.
 */
@Data
public class Definitions {

    /**
     * The tenant of every definition that does not name one.
     */
    private String tenant;
    /**
     * Step definitions shared by the tenant's processes; a process refers to them by step code.
     */
    private List<StepDefinition> steps = new ArrayList<>();
    private List<ProcessDefinition> processes = new ArrayList<>();
    /**
     * Planning rules given as code, by the Java DSL.
     */
    @JsonIgnore
    private List<Rule> rules = new ArrayList<>();

    /**
     * Starts the definitions of a tenant.
     */
    public static DefinitionsBuilder tenant(String tenant) {
        return new DefinitionsBuilder(tenant);
    }

    /**
     * The step and process definitions, each with its tenant set.
     */
    public List<StepDefinition> stepDefinitions() {
        steps.forEach(step -> {
            if (step.getTenant() == null) {
                step.setTenant(tenant);
            }
        });
        return steps;
    }

    public List<ProcessDefinition> processDefinitions() {
        processes.forEach(process -> {
            if (process.getTenant() == null) {
                process.setTenant(tenant);
            }
        });
        return processes;
    }

    /**
     * A planning rule given as code: computes the planned value of one measurement of a step or a process. It applies
     * to its tenant only and, for a step of a process, to that process only: two processes can plan steps of the same
     * code differently.
     */
    @Data
    public static final class Rule {
        private final String tenant;
        private final String measurementCode;
        private final String unit;
        /**
         * Set for a step rule.
         */
        private final String stepCode;
        private final Function<StepInstance, Object> stepRule;
        /**
         * The process of a process rule; for a step rule, the process the step belongs to, or {@code null} for a
         * step shared by the tenant's processes.
         */
        private final String processCode;
        private final Function<ProcessInstance, Object> processRule;
    }
}
