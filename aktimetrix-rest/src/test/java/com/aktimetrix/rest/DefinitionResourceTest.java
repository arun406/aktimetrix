package com.aktimetrix.rest;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.store.DefinitionStore;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class DefinitionResourceTest {

    private final DefinitionStore store = mock(DefinitionStore.class);
    private MockMvc mvc;

    @BeforeEach
    void setUp() {
        final ProcessDefinitionResource processes = new ProcessDefinitionResource(new ProcessDefinitionService(store));
        final StepDefinitionResource steps = new StepDefinitionResource(new StepDefinitionService(store));
        mvc = MockMvcBuilders.standaloneSetup(processes, steps)
                .setControllerAdvice(new InvalidDefinitionHandler()).build();
    }

    @Test
    void aValidProcessDefinitionIsSaved() throws Exception {
        when(store.findProcess("AA", "ORDER_DELIVERY")).thenReturn(Optional.empty());
        when(store.saveProcess(any())).thenAnswer(call -> call.getArgument(0));

        mvc.perform(post("/reference-data/process-definitions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenant\":\"AA\",\"processCode\":\"ORDER_DELIVERY\","
                                + "\"startEventCodes\":[\"ORDER_CREATED_EVENT\"],\"plannedWithin\":\"P1D\"}"))
                .andExpect(status().isCreated());
        verify(store).saveProcess(any(ProcessDefinition.class));
    }

    @Test
    void anInvalidProcessDefinitionIsABadRequestListingItsProblems() throws Exception {
        mvc.perform(post("/reference-data/process-definitions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenant\":\"AA\",\"processCode\":\"ORDER_DELIVERY\",\"plannedWithin\":\"one day\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problems.length()").value(2))
                .andExpect(jsonPath("$.problems[0]").value(
                        "process ORDER_DELIVERY: startEventCodes is missing, so the process can never start"));
        verify(store, never()).saveProcess(any());
    }

    @Test
    void anInvalidStepDefinitionIsABadRequest() throws Exception {
        mvc.perform(post("/reference-data/step-definitions").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"tenant\":\"AA\",\"stepCode\":\"SHIP\",\"optionalInd\":\"maybe\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.problems[0]").value("step SHIP: optionalInd must be Y or N, not maybe"));
        verify(store, never()).saveStep(any());
    }
}
