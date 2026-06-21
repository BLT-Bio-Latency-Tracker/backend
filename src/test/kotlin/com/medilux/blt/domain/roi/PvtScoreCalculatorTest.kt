package com.medilux.blt.domain.roi

import com.medilux.blt.domain.roi.service.PvtScoreCalculator
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class PvtScoreCalculatorTest {
    private val calculator = PvtScoreCalculator()

    @Test
    fun `x at or below 250 yields 100`() {
        assertThat(calculator.calculate(250.0)).isEqualTo(100.0)
        assertThat(calculator.calculate(200.0)).isEqualTo(100.0)
        assertThat(calculator.calculate(0.0)).isEqualTo(100.0)
    }

    @Test
    fun `x at or above 500 yields 0 (boundary is inclusive)`() {
        assertThat(calculator.calculate(500.0)).isEqualTo(0.0)
        assertThat(calculator.calculate(600.0)).isEqualTo(0.0)
    }

    @Test
    fun `midpoint and interior values follow the quadratic falloff`() {
        // ratio 0.5 -> 100 * (1 - 0.25) = 75
        assertThat(calculator.calculate(375.0)).isCloseTo(75.0, within(1e-9))
        // ratio 0.2 -> 100 * (1 - 0.04) = 96
        assertThat(calculator.calculate(300.0)).isCloseTo(96.0, within(1e-9))
        // ratio 0.8 -> 100 * (1 - 0.64) = 36
        assertThat(calculator.calculate(450.0)).isCloseTo(36.0, within(1e-9))
    }

    @Test
    fun `just below upper bound is small but positive`() {
        assertThat(calculator.calculate(499.0)).isGreaterThan(0.0)
    }
}
