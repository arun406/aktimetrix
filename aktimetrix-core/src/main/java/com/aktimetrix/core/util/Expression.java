package com.aktimetrix.core.util;

import java.math.BigDecimal;
import java.math.MathContext;
import java.util.Map;

/**
 * Evaluates arithmetic over named values: numbers, names such as {@code FUEL}, {@code + - * /}, unary minus and
 * parentheses. Nothing else is accepted, so a definition cannot run code.
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

    private final String text;
    private final Map<String, BigDecimal> values;
    private int position;
    private int depth;

    private Expression(String text, Map<String, BigDecimal> values) {
        this.text = text;
        this.values = values;
    }

    /**
     * @return the value, or {@code null} when a name has no value or a division by zero occurs
     * @throws IllegalArgumentException when the expression is not well formed
     */
    public static BigDecimal evaluate(String expression, Map<String, BigDecimal> values) {
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
        while (position < text.length()
                && (Character.isLetterOrDigit(text.charAt(position)) || text.charAt(position) == '_')) {
            position++;
        }
        if (start == position) {
            throw new IllegalArgumentException("Expected a number or a name at " + start + " in " + text);
        }
        final BigDecimal value = values.get(text.substring(start, position));
        if (value == null) {
            throw new MissingValue();
        }
        return value;
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

    private static final class MissingValue extends RuntimeException {
        MissingValue() {
            super(null, null, false, false);
        }
    }
}
