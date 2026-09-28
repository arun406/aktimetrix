package com.aktimetrix.core.util;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
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
}
