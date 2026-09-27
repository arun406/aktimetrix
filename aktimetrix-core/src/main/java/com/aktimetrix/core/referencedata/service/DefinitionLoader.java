package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Loads process and step definitions kept as JSON files with the application's code (by default
 * {@code aktimetrix/process-definitions.json} and {@code aktimetrix/step-definitions.json} on the classpath) and
 * upserts them by tenant and code before any event is consumed.
 */
@Component
@RequiredArgsConstructor
public class DefinitionLoader implements SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(DefinitionLoader.class);

    private final AktimetrixProperties properties;
    private final ObjectMapper objectMapper;
    private final StepDefinitionService stepDefinitionService;
    private final ProcessDefinitionService processDefinitionService;

    @Override
    public void afterSingletonsInstantiated() {
        if (!properties.getDefinitions().isLoadOnStartup()) {
            return;
        }
        final List<StepDefinition> steps = read(properties.getDefinitions().getSteps(), new TypeReference<>() {
        });
        final List<ProcessDefinition> processes = read(properties.getDefinitions().getProcesses(), new TypeReference<>() {
        });
        steps.forEach(stepDefinitionService::add);
        processes.forEach(processDefinitionService::add);
        if (!steps.isEmpty() || !processes.isEmpty()) {
            logger.info("Loaded {} process and {} step definitions", processes.size(), steps.size());
        }
    }

    private <T> List<T> read(String location, TypeReference<List<T>> type) {
        final List<T> definitions = new ArrayList<>();
        try {
            for (Resource resource : new PathMatchingResourcePatternResolver().getResources(location)) {
                try (InputStream in = resource.getInputStream()) {
                    definitions.addAll(objectMapper.readValue(in, type));
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read definitions from " + location, e);
        }
        return definitions;
    }
}
