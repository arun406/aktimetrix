package com.aktimetrix.core.definitions;

import com.aktimetrix.core.api.MeasurementType;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.dataformat.yaml.YAMLFactory;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

import static com.aktimetrix.core.definitions.Planning.metadataTime;
import static org.assertj.core.api.Assertions.assertThat;

class DefinitionsTest {

    private static final String YAML = String.join("\n",
            "tenant: SHOP",
            "steps:",
            "  - stepCode: PACK",
            "    status: CONFIRMED",
            "    startEventCodes: [PACKED]",
            "    plannedWithin: PT1H",
            "processes:",
            "  - processCode: ORDER",
            "    status: CONFIRMED",
            "    entityType: order",
            "    startEventCodes: [ORDER_CREATED]",
            "    cancelEventCodes: [ORDER_CANCELLED]",
            "    plannedWithin: P1D",
            "    measurements:",
            "      - {measurementCode: COST, type: P, value: '8', unit: EUR, tolerance: 10%, worseWhen: HIGHER}",
            "      - {measurementCode: COST, type: A, valueFrom: deliveryCost, unit: EUR}",
            "    metrics:",
            "      - {code: COST_PER_KM, expression: COST / DISTANCE, unit: EUR/KM, tolerance: 10%, worseWhen: HIGHER}",
            "    steps:",
            "      - stepCode: PACK",
            "      - stepCode: TRAVEL",
            "        startEventCodes: [TRAVEL_STARTED]",
            "        endEventCodes: [ARRIVED]",
            "        progressEventCodes: [LOCATION_UPDATED]",
            "        plannedAfter: PACK",
            "        plannedWithin: PT30M",
            "        tolerance: PT5M",
            "        measurements:",
            "          - {measurementCode: DISTANCE, type: P, value: '5', unit: KM, tolerance: 20%, worseWhen: HIGHER}",
            "          - {measurementCode: DISTANCE, type: A, valueFrom: route.distanceKm, unit: KM}",
            "      - stepCode: RATED",
            "        startEventCodes: [RATED]",
            "        optionalInd: Y",
            "");

    @Test
    void theJavaDslAndYamlDescribeTheSameDefinitions() throws Exception {
        final Definitions dsl = Definitions.tenant("SHOP")
                .step("PACK", pack -> pack.on("PACKED").within("PT1H"))
                .process("ORDER", order -> order
                        .entityType("order")
                        .startsOn("ORDER_CREATED")
                        .cancelledOn("ORDER_CANCELLED")
                        .within("P1D")
                        .measure("COST", "deliveryCost", cost -> cost.value(8).unit("EUR").tolerance("10%")
                                .worseWhenHigher())
                        .metric("COST_PER_KM", "COST / DISTANCE", m -> m.unit("EUR/KM").tolerance("10%")
                                .worseWhenHigher())
                        .step("PACK")
                        .step("TRAVEL", travel -> travel.startsOn("TRAVEL_STARTED").endsOn("ARRIVED")
                                .progressOn("LOCATION_UPDATED").after("PACK").within("PT30M").tolerance("PT5M")
                                .measure("DISTANCE", "route.distanceKm", km -> km.value(5).unit("KM")
                                        .tolerance("20%").worseWhenHigher()))
                        .step("RATED", rated -> rated.on("RATED").optional()))
                .build();
        final Definitions yaml = new ObjectMapper(new YAMLFactory()).readValue(YAML, Definitions.class);

        final ObjectMapper json = new ObjectMapper();
        assertThat(json.writeValueAsString(dsl.processDefinitions()))
                .isEqualTo(json.writeValueAsString(yaml.processDefinitions()));
        assertThat(json.writeValueAsString(dsl.stepDefinitions()))
                .isEqualTo(json.writeValueAsString(yaml.stepDefinitions()));
        assertThat(dsl.getRules()).isEmpty();
    }

    @Test
    void aStepWithoutATenantTakesTheTenantOfItsDefinitions() throws Exception {
        final Definitions yaml = new ObjectMapper(new YAMLFactory()).readValue(YAML, Definitions.class);

        assertThat(yaml.stepDefinitions()).extracting(StepDefinition::getTenant).containsOnly("SHOP");
        assertThat(yaml.processDefinitions()).extracting(ProcessDefinition::getTenant).containsOnly("SHOP");
    }

    @Test
    void aPlanningRuleDeclaresItsMeasurementAndIsKeptAsCode() {
        final Definitions dsl = Definitions.tenant("SHOP")
                .process("ORDER", order -> order
                        .step("DELIVERED", delivered -> delivered.on("DELIVERED")
                                .planTime(step -> metadataTime(step, "createdAt").plusHours(4))))
                .build();

        final StepDefinition delivered = dsl.processDefinitions().get(0).getSteps().get(0);
        assertThat(delivered.getMeasurements()).singleElement().satisfies(m -> {
            assertThat(m.getMeasurementCode()).isEqualTo("TIME");
            assertThat(m.getType()).isEqualTo(MeasurementType.P);
            assertThat(m.getValue()).isNull();
        });
        final Definitions.Rule rule = dsl.getRules().get(0);
        assertThat(rule.getStepCode()).isEqualTo("DELIVERED");
        final StepInstance step = new StepInstance();
        step.setStepCode("DELIVERED");
        step.setMetadata(Map.of("createdAt", "2024-03-01 09:00:00"));
        assertThat(RuleMeters.step(rule).measure("SHOP", step).getValue())
                .isEqualTo(LocalDateTime.of(2024, 3, 1, 13, 0).toString());
        assertThat(List.of(RuleMeters.step(rule).measure("SHOP", step).getUnit())).containsExactly("TIMESTAMP");
    }
}
