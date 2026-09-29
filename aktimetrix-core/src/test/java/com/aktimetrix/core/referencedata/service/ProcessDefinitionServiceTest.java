package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.model.ProcessInstance;
import com.aktimetrix.core.store.DefinitionStore;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProcessDefinitionServiceTest {

    @Mock
    private DefinitionStore store;
    @InjectMocks
    private ProcessDefinitionService service;

    @Test
    void resolvesStepsWithinTheTenantInProcessOrder() {
        ProcessDefinition delivery = new ProcessDefinition("AA", "ORDER_DELIVERY");
        delivery.setSteps(List.of(stub("PLACE"), stub("SHIP"), stub("DELIVER")));
        when(store.findConfirmedProcessesStartedBy("AA", "ORDER_PLACED_EVENT")).thenReturn(List.of(delivery));
        StepDefinition place = step("AA", "PLACE");
        StepDefinition ship = step("AA", "SHIP");
        when(store.findStep("AA", "PLACE")).thenReturn(Optional.of(place));
        when(store.findStep("AA", "SHIP")).thenReturn(Optional.of(ship));

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
        when(store.findConfirmedProcessesStartedBy("AA", "ORDER_PLACED_EVENT")).thenReturn(List.of(express));
        when(store.findStep("AA", "SHIP")).thenReturn(Optional.of(shared));
        when(store.findStep("AA", "GIFT_WRAP")).thenReturn(Optional.empty());

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
        existing.setRevision(3L);
        when(store.findProcess("AA", "ORDER_DELIVERY")).thenReturn(Optional.of(existing));
        ProcessDefinition updated = new ProcessDefinition("AA", "ORDER_DELIVERY");
        updated.setPlannedWithin("P1D");

        service.add(updated);

        assertThat(updated.getId()).isEqualTo("42");
        assertThat(updated.getRevision()).as("a changed definition is a new revision").isEqualTo(4L);
        verify(store).saveProcess(updated);
    }

    @Test
    void aNewDefinitionIsRevisionOneAndAnUnchangedOneKeepsItsRevision() {
        ProcessDefinition created = new ProcessDefinition("AA", "ORDER_DELIVERY");
        when(store.findProcess("AA", "ORDER_DELIVERY")).thenReturn(Optional.empty());
        service.add(created);
        assertThat(created.getRevision()).isEqualTo(1L);

        ProcessDefinition stored = new ProcessDefinition("AA", "ORDER_DELIVERY");
        stored.setId("42");
        stored.setRevision(2L);
        stored.setPlannedWithin("P1D");
        when(store.findProcess("AA", "ORDER_DELIVERY")).thenReturn(Optional.of(stored));
        ProcessDefinition reloaded = new ProcessDefinition("AA", "ORDER_DELIVERY");
        reloaded.setPlannedWithin("P1D");
        service.add(reloaded);
        assertThat(reloaded.getRevision()).as("reloading an unchanged definition").isEqualTo(2L);
    }

    @Test
    void aProcessInstanceFollowsTheDefinitionItStartedWith() {
        ProcessDefinition atStart = new ProcessDefinition("AA", "ORDER_DELIVERY");
        atStart.setRevision(1L);
        atStart.setSteps(List.of(step("AA", "PLACE"), step("AA", "SHIP")));
        ProcessInstance instance = new ProcessInstance(atStart);

        assertThat(instance.getDefinitionRevision()).isEqualTo(1L);
        assertThat(service.definitionOf(instance)).isSameAs(atStart);
        assertThat(service.stepDefinitionsOf(instance)).containsOnlyKeys("PLACE", "SHIP");
    }

    @Test
    void anInstanceWithoutADefinitionFollowsTheCurrentOne() {
        ProcessInstance legacy = new ProcessInstance();
        legacy.setTenant("AA");
        legacy.setProcessCode("ORDER_DELIVERY");
        ProcessDefinition current = new ProcessDefinition("AA", "ORDER_DELIVERY");
        current.setSteps(List.of(stub("SHIP")));
        when(store.findProcess("AA", "ORDER_DELIVERY")).thenReturn(Optional.of(current));
        StepDefinition ship = step("AA", "SHIP");
        ship.setPlannedWithin("PT2H");
        when(store.findStep("AA", "SHIP")).thenReturn(Optional.of(ship));

        assertThat(service.stepDefinitionsOf(legacy).get("SHIP").getPlannedWithin()).isEqualTo("PT2H");
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
