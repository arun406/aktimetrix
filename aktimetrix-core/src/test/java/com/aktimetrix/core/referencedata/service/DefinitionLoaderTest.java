package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.definitions.Definitions;
import com.aktimetrix.core.impl.DefaultRegistry;
import com.aktimetrix.core.meter.impl.AbstractMeter;
import com.aktimetrix.core.model.StepInstance;
import com.aktimetrix.core.postbeanprocessors.MeterPostBeanProcessor;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.service.RegistryService;
import com.aktimetrix.core.stereotypes.Measurement;
import tools.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.support.StaticListableBeanFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

class DefinitionLoaderTest {

    private final DefaultRegistry registry = new DefaultRegistry();
    private final RegistryService registryService = new RegistryService();
    private final StepDefinitionService steps = mock(StepDefinitionService.class);
    private final ProcessDefinitionService processes = mock(ProcessDefinitionService.class);

    DefinitionLoaderTest() {
        ReflectionTestUtils.setField(registryService, "registry", registry);
    }

    @Test
    void eachProcessPlansAStepOfTheSameCodeByItsOwnRule() {
        new MeterPostBeanProcessor(registry).postProcessAfterInitialization(new AnyDeliveryMeter(), "anyDelivery");
        final Definitions shop = Definitions.tenant("SHOP")
                .process("EXPRESS", p -> p.startsOn("EXPRESS_ORDERED")
                        .step("DELIVER", s -> s.on("DELIVERED").planTime(step -> at(4))))
                .process("STANDARD", p -> p.startsOn("STANDARD_ORDERED")
                        .step("DELIVER", s -> s.on("DELIVERED").planTime(step -> at(48))))
                .build();

        loader(null, shop).afterSingletonsInstantiated();

        assertThat(plannedTime("SHOP", "EXPRESS")).isEqualTo(at(4).toString());
        assertThat(plannedTime("SHOP", "STANDARD")).isEqualTo(at(48).toString());
        assertThat(plannedTime("SHOP", "RETURN")).as("a process without a rule of its own").isEqualTo("meter");
        assertThat(plannedTime("OTHER", "EXPRESS")).as("another tenant").isEqualTo("meter");
        assertThat(registryService.getMeter("SHOP", "DELIVER", "TIME")).as("actuals ignore rules")
                .isInstanceOf(AnyDeliveryMeter.class);
    }

    @Test
    void aProcessRuleBeatsASharedStepRuleOfTheTenant() {
        final Definitions shop = Definitions.tenant("SHOP")
                .step("DELIVER", s -> s.on("DELIVERED").planTime(step -> at(24)))
                .process("EXPRESS", p -> p.startsOn("EXPRESS_ORDERED")
                        .step("DELIVER", s -> s.planTime(step -> at(4))))
                .process("STANDARD", p -> p.startsOn("STANDARD_ORDERED").step("DELIVER"))
                .build();

        loader(null, shop).afterSingletonsInstantiated();

        assertThat(plannedTime("SHOP", "EXPRESS")).isEqualTo(at(4).toString());
        assertThat(plannedTime("SHOP", "STANDARD")).isEqualTo(at(24).toString());
    }

    @Test
    void twoRulesForTheSameMeasurementInTheSamePlaceFail() {
        final Definitions one = Definitions.tenant("SHOP")
                .process("EXPRESS", p -> p.startsOn("E").step("DELIVER", s -> s.on("D").planTime(step -> at(4))))
                .build();
        final Definitions two = Definitions.tenant("SHOP")
                .process("EXPRESS", p -> p.startsOn("E").step("DELIVER", s -> s.on("D").planTime(step -> at(5))))
                .build();

        assertThatThrownBy(() -> loader(null, one, two).afterSingletonsInstantiated())
                .isInstanceOf(BeanInitializationException.class)
                .hasMessageContaining("Two planning rules for TIME of step DELIVER of process EXPRESS of tenant SHOP");
    }

    @Test
    void aValidYamlFileIsSaved() {
        loader("classpath:definitions-test/valid.yaml").afterSingletonsInstantiated();

        final ArgumentCaptor<ProcessDefinition> saved = ArgumentCaptor.forClass(ProcessDefinition.class);
        verify(processes).add(saved.capture());
        assertThat(saved.getValue().getTenant()).isEqualTo("T1");
        assertThat(saved.getValue().getSteps()).hasSize(2);
    }

    @Test
    void aMisspeltFieldStopsTheApplicationAndSaysWhere() {
        assertThatThrownBy(() -> loader("classpath:definitions-test/misspelt.yaml").afterSingletonsInstantiated())
                .isInstanceOf(BeanInitializationException.class)
                .hasMessageContaining("misspelt.yaml")
                .hasMessageContaining("plannedWitin");
        verify(processes, never()).add(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void invalidValuesAreAllReportedBeforeAnythingIsSaved() {
        assertThatThrownBy(() -> loader("classpath:definitions-test/invalid.yaml").afterSingletonsInstantiated())
                .isInstanceOf(BeanInitializationException.class)
                .hasMessageContaining("invalid.yaml")
                .hasMessageContaining("process PARCEL: startEventCodes is missing")
                .hasMessageContaining("step PICKUP: plannedWithin is not an ISO-8601 duration")
                .hasMessageContaining("step DELIVER: plannedAfter names SORT, which is not a step of the process")
                .hasMessageContaining("process PARCEL: alternative HANDOVER has a single step")
                .hasMessageContaining("measurement WEIGHT: tolerance must be an amount")
                .hasMessageContaining("measurement WEIGHT: worseWhen must be HIGHER or LOWER");
        verify(processes, never()).add(org.mockito.ArgumentMatchers.any());
        verify(steps, never()).add(org.mockito.ArgumentMatchers.any());
    }

    private String plannedTime(String tenant, String processCode) {
        final StepInstance step = new StepInstance();
        step.setStepCode("DELIVER");
        return registryService.planMeter(tenant, processCode, "DELIVER", "TIME").measure(tenant, step).getValue();
    }

    private DefinitionLoader loader(String files, Definitions... beans) {
        final AktimetrixProperties properties = new AktimetrixProperties();
        properties.getDefinitions().setProcesses("classpath*:none/*.json");
        properties.getDefinitions().setSteps("classpath*:none/*.json");
        properties.getDefinitions().setFiles(files);
        final Map<String, Object> named = new LinkedHashMap<>();
        for (int i = 0; i < beans.length; i++) {
            named.put("definitions" + i, beans[i]);
        }
        return new DefinitionLoader(properties, new ObjectMapper(), steps, processes,
                new StaticListableBeanFactory(named).getBeanProvider(Definitions.class), registry);
    }

    private static java.time.Instant at(int hours) {
        return java.time.LocalDateTime.of(2024, 3, 1, 9, 0).toInstant(ZoneOffset.UTC).plus(Duration.ofHours(hours));
    }

    @Measurement(code = "TIME", stepCode = "DELIVER")
    static class AnyDeliveryMeter extends AbstractMeter {
        @Override
        protected String getMeasurementUnit(String tenant, StepInstance step) {
            return "TIMESTAMP";
        }

        @Override
        protected String getMeasurementValue(String tenant, StepInstance step) {
            return "meter";
        }
    }
}
