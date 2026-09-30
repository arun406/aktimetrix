package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.definitions.Definitions;
import com.aktimetrix.core.definitions.RuleMeters;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.service.RegistryService;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.BeanInitializationException;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Loads the process and step definitions before any event is consumed, and upserts them by tenant and code:
 * <ul>
 *     <li>JSON arrays kept with the application's code, by default {@code aktimetrix/process-definitions.json} and
 *     {@code aktimetrix/step-definitions.json} on the classpath;</li>
 *     <li>YAML files, by default {@code aktimetrix/*.yaml} and {@code aktimetrix/*.yml}, each a {@link Definitions};</li>
 *     <li>{@link Definitions} beans built with the Java DSL, whose planning rules it registers as meters.</li>
 * </ul>
 */
@Component
@RequiredArgsConstructor
public class DefinitionLoader implements SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(DefinitionLoader.class);
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .registerModule(new JavaTimeModule())
            .disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final AktimetrixProperties properties;
    private final ObjectMapper objectMapper;
    private final StepDefinitionService stepDefinitionService;
    private final ProcessDefinitionService processDefinitionService;
    private final ObjectProvider<Definitions> definitionBeans;
    private final Registry registry;
    private final RegistryService registryService;

    @Override
    public void afterSingletonsInstantiated() {
        final List<Definitions> beans = definitionBeans.orderedStream().collect(Collectors.toList());
        beans.forEach(definitions -> definitions.getRules().forEach(this::register));
        if (!properties.getDefinitions().isLoadOnStartup()) {
            return;
        }
        final List<StepDefinition> steps = read(properties.getDefinitions().getSteps(), new TypeReference<>() {
        });
        final List<ProcessDefinition> processes = read(properties.getDefinitions().getProcesses(), new TypeReference<>() {
        });
        final List<Definitions> sets = new ArrayList<>(readYaml(properties.getDefinitions().getFiles()));
        sets.addAll(beans);
        for (Definitions set : sets) {
            steps.addAll(set.stepDefinitions());
            processes.addAll(set.processDefinitions());
        }
        steps.forEach(stepDefinitionService::add);
        processes.forEach(processDefinitionService::add);
        if (!steps.isEmpty() || !processes.isEmpty()) {
            logger.info("Loaded {} process and {} step definitions", processes.size(), steps.size());
        }
    }

    private void register(Definitions.Rule rule) {
        final boolean step = rule.getStepCode() != null;
        final String level = step ? rule.getStepCode() : rule.getProcessCode();
        final Object existing = step ? registryService.getMeter(null, level, rule.getMeasurementCode())
                : registryService.getProcessMeter(null, level, rule.getMeasurementCode());
        if (existing != null) {
            throw new BeanInitializationException("Two planning rules for " + rule.getMeasurementCode() + " of "
                    + (step ? "step " : "process ") + level + ": " + existing + " and one in a Definitions bean");
        }
        registry.register("dsl:" + (step ? "step:" : "process:") + level + ":" + rule.getMeasurementCode(),
                new HashMap<>(Map.of(Constants.ATT_METER_SERVICE, Constants.VAL_YES,
                        Constants.ATT_CODE, rule.getMeasurementCode(),
                        Constants.ATT_STEP_CODE, step ? level : "",
                        Constants.ATT_PROCESS_CODE, step ? "" : level)),
                step ? RuleMeters.step(rule) : RuleMeters.process(rule));
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

    private List<Definitions> readYaml(String locations) {
        final List<Definitions> sets = new ArrayList<>();
        if (locations == null) {
            return sets;
        }
        for (String location : locations.split(",")) {
            if (location.isBlank()) {
                continue;
            }
            try {
                for (Resource resource : new PathMatchingResourcePatternResolver().getResources(location.trim())) {
                    try (InputStream in = resource.getInputStream()) {
                        final Definitions set = YAML.readValue(in, Definitions.class);
                        if (set != null) {
                            sets.add(set);
                        }
                    } catch (IOException e) {
                        throw new UncheckedIOException("Cannot read definitions from " + resource, e);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read definitions from " + location, e);
            }
        }
        return sets;
    }
}
