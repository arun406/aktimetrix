package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.api.Registry;
import com.aktimetrix.core.configurations.AktimetrixProperties;
import com.aktimetrix.core.definitions.Definitions;
import com.aktimetrix.core.definitions.RuleMeters;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
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
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Loads the process and step definitions before any event is consumed, and upserts them by tenant and code:
 * <ul>
 *     <li>JSON arrays kept with the application's code, by default {@code aktimetrix/process-definitions.json} and
 *     {@code aktimetrix/step-definitions.json} on the classpath;</li>
 *     <li>YAML files, by default {@code aktimetrix/*.yaml} and {@code aktimetrix/*.yml}, each a {@link Definitions};</li>
 *     <li>{@link Definitions} beans built with the Java DSL, whose planning rules it registers as meters, each limited
 *     to its tenant and, for a step of a process, to that process.</li>
 * </ul>
 * Files are read strictly, an unknown field being an error, and every definition is validated before any is saved:
 * the application does not start with a mistake in them, and the error lists every problem with where it is.
 */
@Component
@RequiredArgsConstructor
public class DefinitionLoader implements SmartInitializingSingleton {
    private static final Logger logger = LoggerFactory.getLogger(DefinitionLoader.class);
    private static final ObjectMapper YAML = new ObjectMapper(new YAMLFactory())
            .registerModule(new JavaTimeModule())
            .enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);

    private final AktimetrixProperties properties;
    private final ObjectMapper objectMapper;
    private final StepDefinitionService stepDefinitionService;
    private final ProcessDefinitionService processDefinitionService;
    private final ObjectProvider<Definitions> definitionBeans;
    private final Registry registry;

    @Override
    public void afterSingletonsInstantiated() {
        final List<Definitions> beans = definitionBeans.orderedStream().collect(Collectors.toList());
        final Set<String> ruleKeys = new HashSet<>();
        beans.forEach(definitions -> definitions.getRules().forEach(rule -> register(rule, ruleKeys)));
        if (!properties.getDefinitions().isLoadOnStartup()) {
            return;
        }
        final Map<String, List<StepDefinition>> steps = new LinkedHashMap<>();
        final Map<String, List<ProcessDefinition>> processes = new LinkedHashMap<>();
        readJson(properties.getDefinitions().getSteps(), new TypeReference<List<StepDefinition>>() {
        }, steps);
        readJson(properties.getDefinitions().getProcesses(), new TypeReference<List<ProcessDefinition>>() {
        }, processes);
        readYaml(properties.getDefinitions().getFiles()).forEach((source, set) -> {
            steps.computeIfAbsent(source, s -> new ArrayList<>()).addAll(set.stepDefinitions());
            processes.computeIfAbsent(source, s -> new ArrayList<>()).addAll(set.processDefinitions());
        });
        for (Definitions set : beans) {
            final String source = "the Definitions bean of tenant " + set.getTenant();
            steps.computeIfAbsent(source, s -> new ArrayList<>()).addAll(set.stepDefinitions());
            processes.computeIfAbsent(source, s -> new ArrayList<>()).addAll(set.processDefinitions());
        }
        validate(steps, processes);
        steps.values().forEach(list -> list.forEach(stepDefinitionService::add));
        processes.values().forEach(list -> list.forEach(processDefinitionService::add));
        final long processCount = processes.values().stream().mapToLong(List::size).sum();
        final long stepCount = steps.values().stream().mapToLong(List::size).sum();
        if (processCount + stepCount > 0) {
            logger.info("Loaded {} process and {} step definitions", processCount, stepCount);
        }
    }

    /**
     * Fails with every problem found, each with the file or bean it is in, before anything is saved.
     */
    private static void validate(Map<String, List<StepDefinition>> steps,
                                 Map<String, List<ProcessDefinition>> processes) {
        final List<String> problems = new ArrayList<>();
        steps.forEach((source, list) -> list.forEach(step -> DefinitionValidator.problems(step)
                .forEach(problem -> problems.add(source + ": " + problem))));
        processes.forEach((source, list) -> list.forEach(process -> DefinitionValidator.problems(process)
                .forEach(problem -> problems.add(source + ": " + problem))));
        if (!problems.isEmpty()) {
            throw new BeanInitializationException("Invalid Aktimetrix definitions:\n  - "
                    + String.join("\n  - ", problems));
        }
    }

    /**
     * Registers a planning rule of the DSL as a meter, limited to its tenant and, for a step of a process, to that
     * process. Rules for the same measurement in the same place conflict; a rule and an {@code @Measurement} meter do
     * not, the rule being more specific.
     */
    private void register(Definitions.Rule rule, Set<String> keys) {
        final boolean step = rule.getStepCode() != null;
        final String place = step
                ? "step " + rule.getStepCode() + (rule.getProcessCode() == null ? "" : " of process " + rule.getProcessCode())
                : "process " + rule.getProcessCode();
        final String key = "dsl:" + rule.getTenant() + ":" + place + ":" + rule.getMeasurementCode();
        if (!keys.add(key)) {
            throw new BeanInitializationException("Two planning rules for " + rule.getMeasurementCode() + " of "
                    + place + " of tenant " + rule.getTenant());
        }
        final Map<String, String> attributes = new HashMap<>(Map.of(Constants.ATT_METER_SERVICE, Constants.VAL_YES,
                Constants.ATT_CODE, rule.getMeasurementCode(),
                Constants.ATT_STEP_CODE, step ? rule.getStepCode() : "",
                Constants.ATT_PROCESS_CODE, step ? "" : rule.getProcessCode()));
        if (rule.getTenant() != null) {
            attributes.put(Constants.ATT_RULE_TENANT, rule.getTenant());
        }
        if (step && rule.getProcessCode() != null) {
            attributes.put(Constants.ATT_RULE_PROCESS, rule.getProcessCode());
        }
        registry.register(key, attributes, step ? RuleMeters.step(rule) : RuleMeters.process(rule));
    }

    private <T> void readJson(String location, TypeReference<List<T>> type, Map<String, List<T>> into) {
        final ObjectMapper strict = objectMapper.copy().enable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
        try {
            for (Resource resource : new PathMatchingResourcePatternResolver().getResources(location)) {
                try (InputStream in = resource.getInputStream()) {
                    into.computeIfAbsent(resource.getDescription(), s -> new ArrayList<>())
                            .addAll(strict.readValue(in, type));
                } catch (IOException e) {
                    throw new BeanInitializationException("Cannot read definitions from " + resource.getDescription()
                            + ": " + e.getMessage(), e);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException("Cannot read definitions from " + location, e);
        }
    }

    private Map<String, Definitions> readYaml(String locations) {
        final Map<String, Definitions> sets = new LinkedHashMap<>();
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
                            sets.put(resource.getDescription(), set);
                        }
                    } catch (IOException e) {
                        throw new BeanInitializationException("Cannot read definitions from "
                                + resource.getDescription() + ": " + e.getMessage(), e);
                    }
                }
            } catch (IOException e) {
                throw new UncheckedIOException("Cannot read definitions from " + location, e);
            }
        }
        return sets;
    }
}
