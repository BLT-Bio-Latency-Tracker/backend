package com.medilux.blt.domain.roi

import com.medilux.blt.domain.roi.entity.CalculationScenario
import com.medilux.blt.domain.roi.entity.RoiQuadrant
import com.medilux.blt.domain.roi.service.BrainRoiCalculator
import com.medilux.blt.domain.roi.service.PvtScoreCalculator
import com.medilux.blt.domain.roi.service.SleepScoreCalculator
import com.medilux.blt.domain.roi.service.SleepScoreInput
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test

class BrainRoiCalculatorTest {
    private val calculator = BrainRoiCalculator(PvtScoreCalculator(), SleepScoreCalculator())

    private fun fullSleep() = SleepScoreInput(
        totalMinutes = 400,
        deepMinutes = 72,
        remMinutes = 88,
        nightHrvMs = 45.2,
        weeklyHrvBaselineMs = 48.1,
        inBedMinutes = 460,
    )

    private val emptySleep = SleepScoreInput(null, null, null, null, null, null)

    @Test
    fun `weighted final with full data, scenario A, high-high quadrant`() {
        // sleep 96, pvt(312) ~= 93.85 -> 96*0.4 + 93.85*0.6 ~= 94.7 -> 95
        val result = calculator.calculate(avgRtMs = 312.0, sleepDataPresent = true, sleepInput = fullSleep())

        assertThat(result.sleepScore).isEqualTo(96)
        assertThat(result.pvtScore).isEqualTo(94)
        assertThat(result.finalScore).isEqualTo(95)
        assertThat(result.scenario).isEqualTo(CalculationScenario.A)
        assertThat(result.quadrant).isEqualTo(RoiQuadrant.HIGH_SLEEP_HIGH_PVT)
        assertThat(result.formulaVersion).isEqualTo("v2")
        assertThat(result.breakdown["finalFormula"]).isEqualTo("WEIGHTED")
        assertThat(result.breakdown["sleepMissing"]).isEqualTo(false)
    }

    @Test
    fun `PVT_ONLY uses exception1 formula and scenario D`() {
        // pvt(312) ~= 93.85 -> (93.85^2/100)*0.2 + 93.85*0.8 ~= 92.7 -> 93
        val result = calculator.calculate(avgRtMs = 312.0, sleepDataPresent = false, sleepInput = emptySleep)

        assertThat(result.scenario).isEqualTo(CalculationScenario.D)
        assertThat(result.sleepScore).isEqualTo(0)
        assertThat(result.sleepScoreNullable).isNull()
        assertThat(result.finalScore).isEqualTo(93)
        assertThat(result.quadrant).isEqualTo(RoiQuadrant.LOW_SLEEP_HIGH_PVT)
        assertThat(result.breakdown["finalFormula"]).isEqualTo("PVT_ONLY_EXCEPTION1")
        assertThat(result.breakdown["sleepMissing"]).isEqualTo(true)
        assertThat(result.breakdown["sleepWeight"]).isNull()
    }

    @Test
    fun `scenario B when only HRV missing`() {
        val result = calculator.calculate(
            avgRtMs = 312.0,
            sleepDataPresent = true,
            sleepInput = fullSleep().copy(nightHrvMs = null),
        )
        assertThat(result.scenario).isEqualTo(CalculationScenario.B)
    }

    @Test
    fun `scenario C when a sleep stage missing`() {
        val result = calculator.calculate(
            avgRtMs = 312.0,
            sleepDataPresent = true,
            sleepInput = fullSleep().copy(deepMinutes = null),
        )
        assertThat(result.scenario).isEqualTo(CalculationScenario.C)
    }

    @Test
    fun `scenario C when sleep submitted but unusable`() {
        // totalMinutes & inBed both null -> score null, but data present -> C (not D)
        val result = calculator.calculate(
            avgRtMs = 312.0,
            sleepDataPresent = true,
            sleepInput = SleepScoreInput(null, null, null, 45.0, 48.0, null),
        )
        assertThat(result.scenario).isEqualTo(CalculationScenario.C)
        assertThat(result.sleepScoreNullable).isNull()
        assertThat(result.breakdown["finalFormula"]).isEqualTo("PVT_ONLY_EXCEPTION1")
    }

    @Test
    fun `scenario C when TST estimated from inBed even if stages present`() {
        // totalMinutes 결측 → inBed로 TST 추정. 단계는 실측이지만 결측 보정이 있었으므로 C.
        val result = calculator.calculate(
            avgRtMs = 312.0,
            sleepDataPresent = true,
            sleepInput = SleepScoreInput(
                totalMinutes = null,
                deepMinutes = 72,
                remMinutes = 88,
                nightHrvMs = 45.2,
                weeklyHrvBaselineMs = 48.1,
                inBedMinutes = 480,
            ),
        )
        assertThat(result.scenario).isEqualTo(CalculationScenario.C)
        assertThat(result.breakdown["tstEstimatedFromInBed"]).isEqualTo(true)
    }

    @Test
    fun `quadrant boundary is 65 inclusive on each axis`() {
        // Slow PVT (avg 450 -> 36) keeps PVT low; full sleep stays high
        val result = calculator.calculate(avgRtMs = 450.0, sleepDataPresent = true, sleepInput = fullSleep())
        assertThat(result.pvtScore).isEqualTo(36)
        assertThat(result.quadrant).isEqualTo(RoiQuadrant.HIGH_SLEEP_LOW_PVT)
    }
}
