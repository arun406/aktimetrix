package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.referencedata.repository.ProcessDefinitionRepository;
import com.aktimetrix.core.referencedata.repository.StepDefinitionRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessDefinitionServiceTest {

    @Mock
    private ProcessDefinitionRepository repository;
    @Mock
    private StepDefinitionRepository stepDefinitionRepository;
    @InjectMocks
    private ProcessDefinitionService service;

    @Test
    void resolvesStepsWithinTheTenantInProcessOrder() {
        ProcessDefinition delivery = new ProcessDefinition("AA", "ORDER_DELIVERY");
        delivery.setSteps(List.of(stub("PLACE"), stub("SHIP"), stub("DELIVER")));
        when(repository.findConfirmedStartedBy("AA", "ORDER_PLACED_EVENT")).thenReturn(List.of(delivery));
        StepDefinition place = step("AA", "PLACE");
        StepDefinition ship = step("AA", "SHIP");
        when(stepDefinitionRepository.findByStepCode("AA", "PLACE")).thenReturn(place);
        when(stepDefinitionRepository.findByStepCode("AA", "SHIP")).thenReturn(ship);

        List<ProcessDefinition> started = service.findStartedBy("AA", "ORDER_PLACED_EVENT");

        assertThat(started).singleElement().satisfies(definition -> {
            assertThat(definition.getSteps()).extracting(StepDefinition::getStepCode)
                    .containsExactly("PLACE", "SHIP", "DELIVER");
            assertThat(definition.getSteps().get(0).getTenant()).isEqualTo("AA");
            assertThat(definition.getSteps().get(1).getTenant()).isEqualTo("AA");
        });
    }

    @Test
    void theProcessOverridesTheSharedStepAndMayDefineStepsOfItsOwn() {
        // SHIP is shared, with 24 hours to ship; the express process ships within 2 hours and adds GIFT_WRAP
        StepDefinition shared = step("AA", "SHIP");
        shared.setStartEventCodes(List.of("ORDER_SHIPPED_EVENT"));
        shared.setPlannedWithin("PT24H");
        StepDefinition expressShip = stub("SHIP");
        expressShip.setPlannedWithin("PT2H");
        StepDefinition giftWrap = stub("GIFT_WRAP");
        giftWrap.setStartEventCodes(List.of("GIFT_WRAPPED_EVENT"));
        ProcessDefinition express = new ProcessDefinition("AA", "EXPRESS_DELIVERY");
        express.setSteps(List.of(giftWrap, expressShip));
        when(repository.findConfirmedStartedBy("AA", "ORDER_PLACED_EVENT")).thenReturn(List.of(express));
        when(stepDefinitionRepository.findByStepCode("AA", "SHIP")).thenReturn(shared);
        when(stepDefinitionRepository.findByStepCode("AA", "GIFT_WRAP")).thenReturn(null);

        List<StepDefinition> steps = service.findStartedBy("AA", "ORDER_PLACED_EVENT").get(0).getSteps();

        assertThat(steps.get(0)).isSameAs(giftWrap);
        assertThat(steps.get(1).getPlannedWithin()).isEqualTo("PT2H");
        assertThat(steps.get(1).getStartEventCodes()).containsExactly("ORDER_SHIPPED_EVENT");
        assertThat(shared.getPlannedWithin()).as("the shared definition is unchanged").isEqualTo("PT24H");
    }

    @Test
    void addReplacesTheDefinitionWithTheSameTenantAndCode() {
        ProcessDefinition existing = new ProcessDefinition("AA", "ORDER_DELIVERY");
        existing.setId("42");
        when(repository.findByTenantAndProcessCode("AA", "ORDER_DELIVERY")).thenReturn(List.of(existing));
        ProcessDefinition updated = new ProcessDefinition("AA", "ORDER_DELIVERY");

        service.add(updated);

        assertThat(updated.getId()).isEqualTo("42");
        verify(repository).save(updated);
    }

    private static StepDefinition stub(String code) {
        StepDefinition step = new StepDefinition();
        step.setStepCode(code);
        return step;
    }

    private static StepDefinition step(String tenant, String code) {
        StepDefinition step = stub(code);
        step.setTenant(tenant);
        return step;
    }
}
