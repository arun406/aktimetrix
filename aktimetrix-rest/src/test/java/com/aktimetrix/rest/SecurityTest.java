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
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * With Spring Security, a reader may query and a writer may also change definitions; anyone else is refused, and the
 * application's own security still asks who the user is.
 */
@SpringBootTest(classes = SecurityTest.Application.class)
@AutoConfigureMockMvc
class SecurityTest {

    private static final String DEFINITION = "{\"tenant\":\"AA\",\"stepCode\":\"SHIP\"}";

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
    private MeasurementTypeDefinitionService measurementTypeDefinitionService;
    @MockitoBean
    private ProcessMigrationService processMigrationService;

    @Autowired
    private MockMvc mvc;

    @Test
    void anAnonymousUserIsAskedToAuthenticate() throws Exception {
        mvc.perform(get("/reference-data/step-definitions")).andExpect(status().isUnauthorized());
    }

    @Test
    void aUserWithoutARoleIsRefused() throws Exception {
        mvc.perform(get("/reference-data/step-definitions").with(user("someone").roles("OTHER")))
                .andExpect(status().isForbidden());
    }

    @Test
    void aReaderMayQueryButNotChange() throws Exception {
        mvc.perform(get("/reference-data/step-definitions").with(user("reader").roles("AKTIMETRIX_READER")))
                .andExpect(status().isOk());
        mvc.perform(get("/process-instances?tenant=AA&entityId=1234").with(user("reader").roles("AKTIMETRIX_READER")))
                .andExpect(status().isOk());
        mvc.perform(post("/reference-data/step-definitions").with(user("reader").roles("AKTIMETRIX_READER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DEFINITION))
                .andExpect(status().isForbidden());
    }

    @Test
    void aWriterMayQueryAndChange() throws Exception {
        when(stepDefinitionService.add(any())).thenAnswer(call -> call.getArgument(0));
        mvc.perform(get("/reference-data/step-definitions").with(user("writer").roles("AKTIMETRIX_WRITER")))
                .andExpect(status().isOk());
        mvc.perform(post("/reference-data/step-definitions").with(user("writer").roles("AKTIMETRIX_WRITER")).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON).content(DEFINITION))
                .andExpect(status().isCreated());
    }

    @Test
    void anAuthorityWithoutTheRolePrefixCounts() throws Exception {
        mvc.perform(get("/reference-data/step-definitions").with(user("service").authorities(
                        () -> "AKTIMETRIX_READER")))
                .andExpect(status().isOk());
    }
}
