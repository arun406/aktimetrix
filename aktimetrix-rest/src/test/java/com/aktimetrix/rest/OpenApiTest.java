package com.aktimetrix.rest;

import com.aktimetrix.autoconfigure.AktimetrixAutoConfiguration;
import com.aktimetrix.core.referencedata.service.MeasurementTypeDefinitionService;
import com.aktimetrix.core.referencedata.service.ProcessDefinitionService;
import com.aktimetrix.core.referencedata.service.StepDefinitionService;
import com.aktimetrix.core.service.ProcessInstanceService;
import com.aktimetrix.core.service.ProcessMigrationService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * The API describes itself with OpenAPI: every endpoint, and the answer to an invalid definition.
 */
@SpringBootTest(classes = OpenApiTest.Application.class)
@AutoConfigureMockMvc
@WithMockUser
class OpenApiTest {

    @SpringBootConfiguration
    @EnableAutoConfiguration(exclude = AktimetrixAutoConfiguration.class)
    static class Application {
    }

    @MockitoBean
    private ProcessInstanceService processInstanceService;
    @MockitoBean
    private ProcessDefinitionService processDefinitionService;
    @MockitoBean
    private StepDefinitionService stepDefinitionService;
    @MockitoBean
    private ProcessMigrationService processMigrationService;
    @MockitoBean
    private MeasurementTypeDefinitionService measurementTypeDefinitionService;

    @Autowired
    private MockMvc mvc;

    @Test
    void theAktimetrixGroupDescribesEveryEndpoint() throws Exception {
        mvc.perform(get("/v3/api-docs/" + AktimetrixRestAutoConfiguration.API_GROUP))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.info.title").value("Aktimetrix API"))
                .andExpect(jsonPath("$.paths['/process-instances'].get").exists())
                .andExpect(jsonPath("$.paths['/reference-data/process-definitions'].get").exists())
                .andExpect(jsonPath("$.paths['/reference-data/process-definitions'].post.responses['400']").exists())
                .andExpect(jsonPath("$.paths['/reference-data/process-definitions/{tenant}/{processCode}/migrations'].post")
                        .exists())
                .andExpect(jsonPath("$.paths['/reference-data/step-definitions'].post").exists())
                .andExpect(jsonPath("$.paths['/reference-data/measurement-type-definitions'].get").exists())
                .andExpect(jsonPath("$.components.schemas.ProcessDefinition").exists())
                .andExpect(jsonPath("$.components.schemas.StepDefinition").exists())
                .andExpect(jsonPath("$.components.schemas.DefinitionProblems.properties.problems").exists());
    }
}
