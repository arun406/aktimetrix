package com.aktimetrix.core.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Evaluates arithmetic over named values: numbers, names such as {@code FUEL}, {@code + - * /}, unary minus,
 * parentheses, and the functions {@code sum}, {@code avg}, {@code min}, {@code max}, {@code count} and {@code abs}.
 * Nothing else is accepted, so a definition cannot run code.
 * <p>
 * A name may have several values, such as a measurement recorded at several steps. On its own it stands for their sum;
 * as an argument of a function, the function sees each value: {@code max(TEMPERATURE)} is the highest, and
 * {@code count(RATING)} how many there are. An expression as an argument counts as one value.
 */
public final class Expression {

    /**
     * Longest expression accepted, in characters.
     */
    public static final int MAX_LENGTH = 1000;
    /**
     * Deepest nesting of parentheses and unary minus accepted, so that an expression cannot exhaust the stack.
     */
    public static final int MAX_DEPTH = 32;

    /**
     * The functions an expression may call.
     */
    public static final Set<String> FUNCTIONS = Set.of("sum", "avg", "min", "max", "count", "abs");

    private final String text;
    private final Map<String, List<BigDecimal>> values;
    private int position;
    private int depth;

    private Expression(String text, Map<String, List<BigDecimal>> values) {
        this.text = text;
        this.values = values;
    }

    /**
     * Evaluates the expression where each name has one value.
     *
     * @return the value, or {@code null} when a name has no value or a division by zero occurs
     * @throws IllegalArgumentException when the expression is not well formed
     */
    public static BigDecimal evaluate(String expression, Map<String, BigDecimal> values) {
        return evaluateAll(expression, new SingleValues(values));
    }

    /**
     * Evaluates the expression where each name may have several values.
     *
     * @return the value, or {@code null} when a name has no value or a division by zero occurs
     * @throws IllegalArgumentException when the expression is not well formed
     */
    public static BigDecimal evaluateAll(String expression, Map<String, List<BigDecimal>> values) {
        if (expression.length() > MAX_LENGTH) {
            throw new IllegalArgumentException("Expression longer than " + MAX_LENGTH + " characters");
        }
        final Expression parser = new Expression(expression, values);
        try {
            final BigDecimal result = parser.sum();
            parser.skipSpaces();
            if (parser.position != expression.length()) {
                throw new IllegalArgumentException("Unexpected '" + expression.charAt(parser.position) + "' in " + expression);
            }
            return result;
        } catch (MissingValue e) {
            return null;
        }
    }

    /**
     * The names the expression refers to, such as its measurement codes; function names are left out.
     */
    public static Set<String> names(String expression) {
        final Matcher matcher = Pattern.compile("[A-Za-z_][A-Za-z0-9_]*\\s*(\\()?")
                .matcher(expression);
        final Set<String> names = new LinkedHashSet<>();
        while (matcher.find()) {
            final String name = matcher.group().replaceAll("[\\s(]", "");
            if (matcher.group(1) == null || !FUNCTIONS.contains(name)) {
                names.add(name);
            }
        }
        return names;
    }

    private BigDecimal sum() {
        BigDecimal value = product();
        while (true) {
            if (take('+')) {
                value = value.add(product());
            } else if (take('-')) {
                value = value.subtract(product());
            } else {
                return value;
            }
        }
    }

    private BigDecimal product() {
        BigDecimal value = factor();
        while (true) {
            if (take('*')) {
                value = value.multiply(factor());
            } else if (take('/')) {
                final BigDecimal divisor = factor();
                if (divisor.signum() == 0) {
                    throw new MissingValue();
                }
                value = value.divide(divisor, MathContext.DECIMAL64);
            } else {
                return value;
            }
        }
    }

    private BigDecimal factor() {
        if (take('-')) {
            enter();
            final BigDecimal value = factor().negate();
            depth--;
            return value;
        }
        if (take('(')) {
            enter();
            final BigDecimal value = sum();
            if (!take(')')) {
                throw new IllegalArgumentException("Missing ')' in " + text);
            }
            depth--;
            return value;
        }
        skipSpaces();
        final int start = position;
        if (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
            while (position < text.length() && (Character.isDigit(text.charAt(position)) || text.charAt(position) == '.')) {
                position++;
            }
            return new BigDecimal(text.substring(start, position));
        }
        final String name = name();
        if (name.isEmpty()) {
            throw new IllegalArgumentException("Expected a number or a name at " + start + " in " + text);
        }
        if (take('(')) {
            return call(name);
        }
        final List<BigDecimal> found = values.get(name);
        if (found == null || found.isEmpty()) {
            throw new MissingValue();
        }
        return found.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private String name() {
        skipSpaces();
        final int start = position;
        while (position < text.length()
                && (Character.isLetterOrDigit(text.charAt(position)) || text.charAt(position) == '_')) {
            position++;
        }
        return text.substring(start, position);
    }

    /**
     * A function call, after its opening parenthesis.
     */
    private BigDecimal call(String function) {
        if (!FUNCTIONS.contains(function)) {
            throw new IllegalArgumentException("Unknown function " + function + " in " + text + "; expected one of "
                    + FUNCTIONS.stream().sorted().collect(Collectors.joining(", ")));
        }
        enter();
        final List<BigDecimal> arguments = new ArrayList<>();
        int count = 0;
        do {
            arguments.addAll(argument());
            count++;
        } while (take(','));
        if (!take(')')) {
            throw new IllegalArgumentException("Missing ')' after the arguments of " + function + " in " + text);
        }
        depth--;
        if ("count".equals(function)) {
            return BigDecimal.valueOf(arguments.size());
        }
        if (arguments.isEmpty()) {
            throw new MissingValue();
        }
        switch (function) {
            case "sum":
                return arguments.stream().reduce(BigDecimal.ZERO, BigDecimal::add);
            case "avg":
                return arguments.stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                        .divide(BigDecimal.valueOf(arguments.size()), MathContext.DECIMAL64);
            case "min":
                return arguments.stream().min(BigDecimal::compareTo).orElseThrow();
            case "max":
                return arguments.stream().max(BigDecimal::compareTo).orElseThrow();
            default:   // abs
                if (count != 1) {
                    throw new IllegalArgumentException("abs takes one argument, in " + text);
                }
                return arguments.stream().reduce(BigDecimal.ZERO, BigDecimal::add).abs();
        }
    }

    /**
     * The values of one argument: every value of a bare name, or the value of an expression.
     */
    private List<BigDecimal> argument() {
        final int start = position;
        final String name = name();
        if (!name.isEmpty() && !Character.isDigit(name.charAt(0))) {
            skipSpaces();
            if (position < text.length() && (text.charAt(position) == ',' || text.charAt(position) == ')')) {
                final List<BigDecimal> found = values.get(name);
                return found == null ? List.of() : found;
            }
        }
        position = start;
        return List.of(sum());
    }

    private void enter() {
        if (++depth > MAX_DEPTH) {
            throw new IllegalArgumentException("Expression nested deeper than " + MAX_DEPTH + " levels");
        }
    }

    private boolean take(char expected) {
        skipSpaces();
        if (position < text.length() && text.charAt(position) == expected) {
            position++;
            return true;
        }
        return false;
    }

    private void skipSpaces() {
        while (position < text.length() && Character.isWhitespace(text.charAt(position))) {
            position++;
        }
    }

    /**
     * One value per name.
     */
    private static final class SingleValues extends AbstractMap<String, List<BigDecimal>> {
        private final Map<String, BigDecimal> values;

        SingleValues(Map<String, BigDecimal> values) {
            this.values = values;
        }

        @Override
        public List<BigDecimal> get(Object key) {
            final BigDecimal value = values.get(key);
            return value == null ? null : List.of(value);
        }

        @Override
        public Set<Entry<String, List<BigDecimal>>> entrySet() {
            return values.entrySet().stream()
                    .map(e -> Map.entry(e.getKey(), List.of(e.getValue())))
                    .collect(Collectors.toSet());
        }
    }

    private static final class MissingValue extends RuntimeException {
        MissingValue() {
            super(null, null, false, false);
        }
    }
}
