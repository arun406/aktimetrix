package com.aktimetrix.core.referencedata.service;

import com.aktimetrix.core.api.Constants;
import com.aktimetrix.core.referencedata.model.MeasurementDefinition;
import com.aktimetrix.core.referencedata.model.MetricDefinition;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.core.util.Expression;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeParseException;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Internal: checks definitions before they are saved, whether loaded at startup or sent to the REST API, so that a
 * mistake is reported with a message that says where it is, instead of showing only when the first event arrives. Not
 * part of the public API.
 */
final class DefinitionValidator {

    private static final Pattern AMOUNT = Pattern.compile("[-+]?\\d+(\\.\\d+)?%?");
    private static final Set<String> DIRECTIONS = Set.of("HIGHER", "LOWER");

    private DefinitionValidator() {
    }

    /**
     * What is wrong with the process definition, each as a sentence naming the field; empty when it is valid.
     */
    static List<String> problems(ProcessDefinition process) {
        final List<String> problems = new ArrayList<>();
        final String where = "process " + (blank(process.getProcessCode()) ? "(no code)" : process.getProcessCode());
        if (blank(process.getProcessCode())) {
            problems.add(where + ": processCode is missing");
        }
        if (blank(process.getTenant())) {
            problems.add(where + ": tenant is missing");
        }
        if (process.getStartEventCodes() == null || process.getStartEventCodes().isEmpty()) {
            problems.add(where + ": startEventCodes is missing, so the process can never start");
        }
        duration(problems, where, "plannedWithin", process.getPlannedWithin());
        duration(problems, where, "tolerance", process.getTolerance());
        measurements(problems, where, process.getMeasurements());
        if (process.getMetrics() != null) {
            for (MetricDefinition metric : process.getMetrics()) {
                final String at = where + ", metric " + metric.getCode();
                if (blank(metric.getCode())) {
                    problems.add(at + ": code is missing");
                }
                if (blank(metric.getExpression())) {
                    problems.add(at + ": expression is missing");
                } else {
                    expression(problems, at, metric.getExpression());
                }
                amount(problems, at, metric.getTolerance());
                direction(problems, at, metric.getWorseWhen());
            }
        }
        final Set<String> codes = new HashSet<>();
        if (process.getSteps() != null) {
            for (StepDefinition step : process.getSteps()) {
                if (step.getStepCode() != null && !codes.add(step.getStepCode())) {
                    problems.add(where + ": step " + step.getStepCode() + " is listed twice");
                }
            }
            for (StepDefinition step : process.getSteps()) {
                final String at = where + ", step " + step.getStepCode();
                problems.addAll(stepProblems(step, at));
                if (step.getPlannedAfter() != null && !codes.contains(step.getPlannedAfter())) {
                    problems.add(at + ": plannedAfter names " + step.getPlannedAfter()
                            + ", which is not a step of the process");
                }
            }
            process.getSteps().stream().filter(step -> step.getAlternative() != null)
                    .collect(Collectors.groupingBy(StepDefinition::getAlternative, TreeMap::new, Collectors.counting()))
                    .forEach((alternative, count) -> {
                        if (count < 2) {
                            problems.add(where + ": alternative " + alternative
                                    + " has a single step; alternatives need two or more");
                        }
                    });
        }
        return problems;
    }

    /**
     * What is wrong with a step shared by a tenant's processes; empty when it is valid.
     */
    static List<String> problems(StepDefinition step) {
        final List<String> problems = new ArrayList<>(stepProblems(step, "step " + step.getStepCode()));
        if (blank(step.getTenant())) {
            problems.add("step " + step.getStepCode() + ": tenant is missing");
        }
        return problems;
    }

    private static List<String> stepProblems(StepDefinition step, String where) {
        final List<String> problems = new ArrayList<>();
        if (blank(step.getStepCode())) {
            problems.add(where + ": stepCode is missing");
        }
        if (step.getOptionalInd() != null && !"Y".equals(step.getOptionalInd()) && !"N".equals(step.getOptionalInd())) {
            problems.add(where + ": optionalInd must be Y or N, not " + step.getOptionalInd());
        }
        duration(problems, where, "plannedWithin", step.getPlannedWithin());
        duration(problems, where, "tolerance", step.getTolerance());
        measurements(problems, where, step.getMeasurements());
        return problems;
    }

    private static void measurements(List<String> problems, String where, List<MeasurementDefinition> measurements) {
        if (measurements == null) {
            return;
        }
        for (MeasurementDefinition measurement : measurements) {
            final String at = where + ", measurement " + measurement.getMeasurementCode();
            if (blank(measurement.getMeasurementCode())) {
                problems.add(at + ": measurementCode is missing");
            }
            if (measurement.getType() == null) {
                problems.add(at + ": type is missing: P for planned, A for actual");
            }
            if (!Constants.MEASUREMENT_CODE_TIME.equals(measurement.getMeasurementCode())) {
                amount(problems, at, measurement.getTolerance());
            }
            direction(problems, at, measurement.getWorseWhen());
        }
    }

    /**
     * Checks the expression's form with every name valued 1, so that a mistake shows now rather than when a process
     * completes.
     */
    private static void expression(List<String> problems, String where, String expression) {
        try {
            Expression.evaluate(expression, new OneForEveryName());
        } catch (IllegalArgumentException e) {
            problems.add(where + ": expression " + e.getMessage());
        }
    }

    /**
     * Values every name 1.
     */
    private static final class OneForEveryName extends AbstractMap<String, BigDecimal> {
        @Override
        public BigDecimal get(Object key) {
            return BigDecimal.ONE;
        }

        @Override
        public boolean containsKey(Object key) {
            return true;
        }

        @Override
        public Set<Entry<String, BigDecimal>> entrySet() {
            return Set.of();
        }
    }

    private static void duration(List<String> problems, String where, String field, String value) {
        if (value == null) {
            return;
        }
        try {
            Duration.parse(value);
        } catch (DateTimeParseException e) {
            problems.add(where + ": " + field + " is not an ISO-8601 duration such as PT2H or P1D: " + value);
        }
    }

    private static void amount(List<String> problems, String where, String tolerance) {
        if (tolerance != null && !AMOUNT.matcher(tolerance.trim()).matches()) {
            problems.add(where + ": tolerance must be an amount such as 5 or a percentage such as 10%, not "
                    + tolerance);
        }
    }

    private static void direction(List<String> problems, String where, String worseWhen) {
        if (worseWhen != null && !DIRECTIONS.contains(worseWhen.trim().toUpperCase())) {
            problems.add(where + ": worseWhen must be HIGHER or LOWER, not " + worseWhen);
        }
    }

    private static boolean blank(String value) {
        return value == null || value.isBlank();
    }
}
