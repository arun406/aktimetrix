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
            assertThat(definition.getSteps().get(0)).isSameAs(place);
            assertThat(definition.getSteps().get(1)).isSameAs(ship);
        });
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
