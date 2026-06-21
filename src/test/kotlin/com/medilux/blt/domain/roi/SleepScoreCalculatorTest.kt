package com.medilux.blt.domain.roi

import com.medilux.blt.domain.roi.service.SleepPersonalBaseline
import com.medilux.blt.domain.roi.service.SleepScoreCalculator
import com.medilux.blt.domain.roi.service.SleepScoreInput
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.within
import org.junit.jupiter.api.Test

class SleepScoreCalculatorTest {
    private val calculator = SleepScoreCalculator()

    private fun input(
        totalMinutes: Int? = 400,
        deepMinutes: Int? = 72,
        remMinutes: Int? = 88,
        nightHrvMs: Double? = 45.2,
        weeklyHrvBaselineMs: Double? = 48.1,
        inBedMinutes: Int? = 460,
        baseline: SleepPersonalBaseline? = null,
    ) = SleepScoreInput(totalMinutes, deepMinutes, remMinutes, nightHrvMs, weeklyHrvBaselineMs, inBedMinutes, baseline)

    // --- 정상/기본식 ---------------------------------------------------------

    @Test
    fun `full data composes deep rem hrv with 0_4 0_3 0_3 weights`() {
        // deep: ratio 18 -> 90 (72>=45 무페널티); rem: ratio 22 -> 100; hrv: var 93.97 -> 100
        // 90*0.4 + 100*0.3 + 100*0.3 = 96
        val result = calculator.calculate(input())

        assertThat(result.scoreDeep).isCloseTo(90.0, within(1e-9))
        assertThat(result.scoreRem).isCloseTo(100.0, within(1e-9))
        assertThat(result.scoreHrv).isCloseTo(100.0, within(1e-9))
        assertThat(result.score).isCloseTo(96.0, within(1e-9))
        assertThat(result.hrvMissing).isFalse()
        assertThat(result.deepEstimated).isFalse()
        assertThat(result.remEstimated).isFalse()
    }

    @Test
    fun `deep penalty applies when DST below 45 minutes`() {
        // DST 40 of 400 -> ratio 10 -> 50, penalty *0.8 -> 40
        assertThat(calculator.calculate(input(deepMinutes = 40)).scoreDeep).isCloseTo(40.0, within(1e-9))
    }

    @Test
    fun `deep ratio at or above 20 caps at 100`() {
        assertThat(calculator.calculate(input(deepMinutes = 100)).scoreDeep).isCloseTo(100.0, within(1e-9)) // ratio 25
    }

    @Test
    fun `rem score across all four bands`() {
        assertThat(calculator.calculate(input(remMinutes = 40)).scoreRem).isCloseTo(50.0, within(1e-9)) // ratio 10 -> *5
        assertThat(calculator.calculate(input(remMinutes = 88)).scoreRem).isCloseTo(100.0, within(1e-9)) // ratio 22
        assertThat(calculator.calculate(input(remMinutes = 104)).scoreRem).isCloseTo(96.0, within(1e-9)) // ratio 26 -> 100-4
        assertThat(calculator.calculate(input(remMinutes = 140)).scoreRem).isCloseTo(30.0, within(1e-9)) // ratio 35 -> 80-50
    }

    @Test
    fun `rem score floors at 0 when ratio far above 30`() {
        assertThat(calculator.calculate(input(remMinutes = 200)).scoreRem).isEqualTo(0.0) // ratio 50 -> 80-200 -> 0
    }

    @Test
    fun `hrv var bands`() {
        assertThat(calculator.calculate(input(nightHrvMs = 50.0, weeklyHrvBaselineMs = 50.0)).scoreHrv)
            .isCloseTo(100.0, within(1e-9)) // var 100
        assertThat(calculator.calculate(input(nightHrvMs = 35.0, weeklyHrvBaselineMs = 50.0)).scoreHrv)
            .isCloseTo(70.0, within(1e-9)) // var 70
        assertThat(calculator.calculate(input(nightHrvMs = 20.0, weeklyHrvBaselineMs = 50.0)).scoreHrv)
            .isEqualTo(0.0) // var 40 -> 0
    }

    // --- 예외처리 1 ----------------------------------------------------------

    @Test
    fun `exception1 fixed 10 when TST below 180`() {
        val result = calculator.calculate(input(totalMinutes = 150))
        assertThat(result.score).isEqualTo(10.0)
        assertThat(result.fixedLowSleep).isTrue()
    }

    @Test
    fun `case1 NHRV missing uses no-hrv weighting`() {
        // deep 90, rem 100 -> 90*0.571 + 100*0.429 = 94.29
        val result = calculator.calculate(input(nightHrvMs = null))
        assertThat(result.hrvMissing).isTrue()
        assertThat(result.scoreHrv).isNull()
        assertThat(result.score).isCloseTo(94.29, within(0.01))
    }

    // --- 예외처리 2: 케이스 2 (단계 결측) -----------------------------------

    @Test
    fun `case2 DST missing without baseline uses default ratio as estimated minutes`() {
        // 추정 DST = TST*0.15 = 60분 -> scoreDeep 공식: ratio 15 -> 75 (60>=45 무페널티)
        // rem 100, hrv 100 -> 75*0.4 + 100*0.3 + 100*0.3 = 90
        val result = calculator.calculate(input(deepMinutes = null))
        assertThat(result.deepEstimated).isTrue()
        assertThat(result.deepFromBaseline).isFalse()
        assertThat(result.scoreDeep).isCloseTo(75.0, within(1e-9))
        assertThat(result.score).isCloseTo(90.0, within(1e-9))
    }

    @Test
    fun `case2 DST missing with reliable baseline uses personal ratio`() {
        // avgDeepRatio 0.20, TST 400 -> estimated DST 80 -> ratio 20 -> deep 100 (>=45 무페널티)
        val baseline = SleepPersonalBaseline(
            avgTstMinutes = null,
            avgDeepRatio = 0.20,
            avgRemRatio = null,
            tstNights = 0,
            deepNights = 5,
            remNights = 0,
        )
        val result = calculator.calculate(input(deepMinutes = null, baseline = baseline))
        assertThat(result.deepEstimated).isTrue()
        assertThat(result.deepFromBaseline).isTrue()
        assertThat(result.scoreDeep).isCloseTo(100.0, within(1e-9))
        assertThat(result.score).isCloseTo(100.0, within(1e-9))
    }

    @Test
    fun `baseline below min nights falls back to default ratio`() {
        val baseline = SleepPersonalBaseline(
            avgTstMinutes = null,
            avgDeepRatio = 0.20,
            avgRemRatio = null,
            tstNights = 0,
            deepNights = 2,
            remNights = 0, // < MIN_BASELINE_NIGHTS(3)
        )
        val result = calculator.calculate(input(deepMinutes = null, baseline = baseline))
        assertThat(result.deepFromBaseline).isFalse()
        assertThat(result.scoreDeep).isCloseTo(75.0, within(1e-9)) // 추정 DST 60분 -> ratio 15 -> 75
    }

    // --- 예외처리 2: 케이스 3 (양쪽 결측) -----------------------------------

    @Test
    fun `case3 both stages missing without baseline uses TST over 420 clamped`() {
        val result = calculator.calculate(input(totalMinutes = 210, deepMinutes = null, remMinutes = null))
        assertThat(result.score).isCloseTo(50.0, within(1e-9)) // 210/420*100
        assertThat(result.deepEstimated).isTrue()
        assertThat(result.remEstimated).isTrue()
    }

    @Test
    fun `case3 clamps high to 100`() {
        val result = calculator.calculate(input(totalMinutes = 500, deepMinutes = null, remMinutes = null))
        assertThat(result.score).isCloseTo(100.0, within(1e-9)) // 500/420*100=119 -> 100
    }

    @Test
    fun `both stages missing but reliable baseline estimates both via personal ratio`() {
        // avgDeepRatio 0.18, avgRemRatio 0.22, TST 400 -> DST 72(deep 90), RST 88(rem 100), hrv 100 -> 96
        val baseline = SleepPersonalBaseline(
            avgTstMinutes = null,
            avgDeepRatio = 0.18,
            avgRemRatio = 0.22,
            tstNights = 0,
            deepNights = 5,
            remNights = 5,
        )
        val result = calculator.calculate(input(deepMinutes = null, remMinutes = null, baseline = baseline))
        assertThat(result.deepFromBaseline).isTrue()
        assertThat(result.remFromBaseline).isTrue()
        assertThat(result.score).isCloseTo(96.0, within(1e-9))
    }

    // --- 예외처리 2: 케이스 4/5/6 (TST 결측) -------------------------------

    @Test
    fun `case4 TST estimated from inBed when totalMinutes missing`() {
        // inBed 480 -> TST 408; 단계 없음·baseline 없음 -> case3: 408/420*100
        val result = calculator.calculate(
            input(totalMinutes = null, deepMinutes = null, remMinutes = null, inBedMinutes = 480),
        )
        assertThat(result.tstEstimatedFromInBed).isTrue()
        assertThat(result.resolvedTst).isCloseTo(408.0, within(1e-9))
        assertThat(result.score).isCloseTo(97.14, within(0.01))
    }

    @Test
    fun `case5 TST from baseline when total and inBed missing`() {
        // avgTstMinutes 420, 단계 baseline 없음 -> case3: 420/420*100 = 100
        val baseline = SleepPersonalBaseline(
            avgTstMinutes = 420.0,
            avgDeepRatio = null,
            avgRemRatio = null,
            tstNights = 5,
            deepNights = 0,
            remNights = 0,
        )
        val result = calculator.calculate(
            input(totalMinutes = null, deepMinutes = null, remMinutes = null, inBedMinutes = null, baseline = baseline),
        )
        assertThat(result.tstEstimatedFromBaseline).isTrue()
        assertThat(result.resolvedTst).isCloseTo(420.0, within(1e-9))
        assertThat(result.score).isCloseTo(100.0, within(1e-9))
    }

    @Test
    fun `case5 no TST no inBed no baseline yields null score`() {
        val result = calculator.calculate(
            input(totalMinutes = null, deepMinutes = null, remMinutes = null, inBedMinutes = null),
        )
        assertThat(result.score).isNull()
    }

    @Test
    fun `case6 no data at all yields null score`() {
        val result = calculator.calculate(SleepScoreInput(null, null, null, null, null, null))
        assertThat(result.score).isNull()
    }
}
