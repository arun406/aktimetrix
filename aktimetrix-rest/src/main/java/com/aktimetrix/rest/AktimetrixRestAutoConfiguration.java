package com.aktimetrix.rest;

import com.aktimetrix.autoconfigure.AktimetrixAutoConfiguration;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.info.License;
import org.springdoc.core.models.GroupedOpenApi;
import org.springframework.boot.autoconfigure.AutoConfiguration;
import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;

/**
 * The Aktimetrix REST API, in a servlet web application. It is described with OpenAPI as the group
 * {@code aktimetrix}: {@code /v3/api-docs/aktimetrix}, and in Swagger UI when the application adds it.
 */
@AutoConfiguration(after = AktimetrixAutoConfiguration.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
@Import({ProcessInstanceResource.class, ProcessDefinitionResource.class, StepDefinitionResource.class,
        MeasurementTypeDefinitionsResource.class, InvalidDefinitionHandler.class})
public class AktimetrixRestAutoConfiguration {

    /**
     * Name of the OpenAPI group of the Aktimetrix API.
     */
    public static final String API_GROUP = "aktimetrix";

    /**
     * When the application uses Spring Security, every endpoint requires a role: see {@link AktimetrixRestAccess}.
     * Authentication stays with the application's own configuration, which keeps securing its other endpoints.
     */
    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(name = "org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity")
    @EnableMethodSecurity
    static class Security {

        @Bean
        AktimetrixRestAccess aktimetrixRestAccess() {
            return new AktimetrixRestAccess();
        }
    }

    @Configuration(proxyBeanMethods = false)
    @ConditionalOnClass(GroupedOpenApi.class)
    static class OpenApi {

        @Bean
        GroupedOpenApi aktimetrixApi() {
            final String version = AktimetrixRestAutoConfiguration.class.getPackage().getImplementationVersion();
            return GroupedOpenApi.builder()
                    .group(API_GROUP)
                    .displayName("Aktimetrix")
                    .packagesToScan(AktimetrixRestAutoConfiguration.class.getPackageName())
                    .addOpenApiCustomizer(openApi -> openApi.info(new Info()
                            .title("Aktimetrix API")
                            .description("Where each business entity stands against its plan, and the definitions "
                                    + "of the processes, steps and measurements it is planned by.")
                            .version(version == null ? "development" : version)
                            .license(new License().name("Apache License 2.0")
                                    .url("https://www.apache.org/licenses/LICENSE-2.0"))))
                    .build();
        }
    }
}
