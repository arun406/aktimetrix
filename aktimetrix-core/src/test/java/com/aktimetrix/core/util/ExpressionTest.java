package com.aktimetrix.core.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ExpressionTest {

    private static final Map<String, BigDecimal> VALUES = Map.of("FUEL", new BigDecimal("1.0"),
            "DISTANCE", new BigDecimal("12"), "LEG_1", new BigDecimal("4"), "LEG_2", new BigDecimal("8"));

    @Test
    void evaluatesArithmeticOverMeasurementCodes() {
        assertThat(Expression.evaluate("FUEL / DISTANCE * 100", VALUES)).isEqualByComparingTo("8.333333333333333");
        assertThat(Expression.evaluate("(LEG_1 + LEG_2) - DISTANCE", VALUES)).isEqualByComparingTo("0");
        assertThat(Expression.evaluate("-LEG_1 * 2.5", VALUES)).isEqualByComparingTo("-10");
    }

    @Test
    void hasNoValueWhenAMeasurementIsMissingOrADivisorIsZero() {
        assertThat(Expression.evaluate("FUEL / RATING", VALUES)).isNull();
        assertThat(Expression.evaluate("FUEL / (LEG_1 - 4)", VALUES)).isNull();
    }

    @Test
    void rejectsAnythingButArithmetic() {
        assertThatThrownBy(() -> Expression.evaluate("FUEL; System.exit(0)", VALUES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Expression.evaluate("(FUEL", VALUES)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void refusesExpressionsNestedOrLongEnoughToExhaustTheStack() {
        final String nested = "(".repeat(5000) + "FUEL" + ")".repeat(5000);
        assertThatThrownBy(() -> Expression.evaluate(nested, VALUES)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("longer than");
        final String deep = "(".repeat(Expression.MAX_DEPTH + 1) + "FUEL" + ")".repeat(Expression.MAX_DEPTH + 1);
        assertThatThrownBy(() -> Expression.evaluate(deep, VALUES)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("nested deeper than");
        assertThatThrownBy(() -> Expression.evaluate("-".repeat(Expression.MAX_DEPTH + 1) + "FUEL", VALUES))
                .isInstanceOf(IllegalArgumentException.class);
        final String allowed = "(".repeat(Expression.MAX_DEPTH) + "FUEL" + ")".repeat(Expression.MAX_DEPTH);
        assertThat(Expression.evaluate(allowed, VALUES)).isEqualByComparingTo("1");
    }

    @Test
    void functionsAggregateEachValueOfAName() {
        final Map<String, List<BigDecimal>> values = Map.of(
                "TEMPERATURE", List.of(new BigDecimal("21"), new BigDecimal("34"), new BigDecimal("28")),
                "RATING", List.of(new BigDecimal("4")));
        assertThat(Expression.evaluateAll("TEMPERATURE", values)).as("a bare name is the sum").isEqualByComparingTo("83");
        assertThat(Expression.evaluateAll("max(TEMPERATURE)", values)).isEqualByComparingTo("34");
        assertThat(Expression.evaluateAll("min(TEMPERATURE)", values)).isEqualByComparingTo("21");
        assertThat(Expression.evaluateAll("avg(TEMPERATURE)", values)).isEqualByComparingTo("27.66666666666667");
        assertThat(Expression.evaluateAll("count(TEMPERATURE)", values)).isEqualByComparingTo("3");
        assertThat(Expression.evaluateAll("count(DISTANCE)", values)).as("no values").isEqualByComparingTo("0");
        assertThat(Expression.evaluateAll("max(TEMPERATURE, RATING * 10)", values)).isEqualByComparingTo("40");
        assertThat(Expression.evaluateAll("abs(RATING - max(TEMPERATURE))", values)).isEqualByComparingTo("30");
        assertThat(Expression.evaluateAll("max(DISTANCE)", values)).as("a missing measurement").isNull();
    }

    @Test
    void rejectsUnknownFunctionsAndListsTheNamesAnExpressionUses() {
        assertThatThrownBy(() -> Expression.evaluate("median(FUEL)", VALUES)).isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Unknown function median");
        assertThatThrownBy(() -> Expression.evaluate("abs(FUEL, DISTANCE)", VALUES))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Expression.names("max(TEMPERATURE) / avg(TEMPERATURE) + FUEL_2 * abs ( DISTANCE )"))
                .containsExactly("TEMPERATURE", "FUEL_2", "DISTANCE");
    }
}
