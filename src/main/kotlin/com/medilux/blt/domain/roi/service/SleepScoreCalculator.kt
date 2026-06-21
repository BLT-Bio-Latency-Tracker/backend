package com.medilux.blt.domain.roi.service

import org.springframework.stereotype.Component

/** Score_Sleep 산출기 입력 */
data class SleepScoreInput(
    val totalMinutes: Int?,
    val deepMinutes: Int?,
    val remMinutes: Int?,
    val nightHrvMs: Double?,
    val weeklyHrvBaselineMs: Double?,
    val inBedMinutes: Int?,
    /** 최근 7일 개인 기준선. 없으면 결측 시 기본비율 fallback. */
    val baseline: SleepPersonalBaseline? = null,
)

/**
 * 최근 7일 개인 수면 기준선.
 * - avgDeepRatio/avgRemRatio: 야간별 (DST/TST)·(RST/TST) 비율의 평균(0~1).
 * - *Nights: 각 지표의 유효 야간 수(신뢰도 게이트용).
 */
data class SleepPersonalBaseline(
    val avgTstMinutes: Double?,
    val avgDeepRatio: Double?,
    val avgRemRatio: Double?,
    val tstNights: Int,
    val deepNights: Int,
    val remNights: Int,
)

/** Score_Sleep 산출 결과 */
data class SleepScoreResult(
    val score: Double?,
    val scoreDeep: Double?,
    val scoreRem: Double?,
    val scoreHrv: Double?,
    val resolvedTst: Double?,
    val tstEstimatedFromInBed: Boolean = false,
    val tstEstimatedFromBaseline: Boolean = false,
    val deepEstimated: Boolean = false,
    val remEstimated: Boolean = false,
    val deepFromBaseline: Boolean = false,
    val remFromBaseline: Boolean = false,
    val hrvMissing: Boolean = false,
    val fixedLowSleep: Boolean = false,
)

/**
 * Score_Sleep 산출기
 */
@Component
class SleepScoreCalculator {
    private data class StageEstimate(val minutes: Double, val estimated: Boolean, val fromBaseline: Boolean)

    fun calculate(input: SleepScoreInput): SleepScoreResult {
        val baseline = input.baseline
        val tstBaselineOk = baseline?.avgTstMinutes != null && baseline.tstNights >= MIN_BASELINE_NIGHTS
        val deepBaselineOk = baseline?.avgDeepRatio != null && baseline.deepNights >= MIN_BASELINE_NIGHTS
        val remBaselineOk = baseline?.avgRemRatio != null && baseline.remNights >= MIN_BASELINE_NIGHTS

        // 1) TST 결정: 실측 → inBed*0.85 → 7일 평균 → 산출 불가
        var tstEstimatedFromInBed = false
        var tstEstimatedFromBaseline = false
        val tst: Double? = when {
            input.totalMinutes != null && input.totalMinutes > 0 -> input.totalMinutes.toDouble()

            input.inBedMinutes != null && input.inBedMinutes > 0 -> {
                tstEstimatedFromInBed = true
                input.inBedMinutes * IN_BED_TO_TST_RATIO
            }

            tstBaselineOk -> {
                tstEstimatedFromBaseline = true
                baseline!!.avgTstMinutes!!
            }

            else -> null
        }

        // 예외처리 1-1) TST 산출 불가 → Score_Sleep 산출 불가
        if (tst == null) {
            return SleepScoreResult(
                score = null,
                scoreDeep = null,
                scoreRem = null,
                scoreHrv = null,
                resolvedTst = null,
                hrvMissing = input.nightHrvMs == null || input.weeklyHrvBaselineMs == null,
            )
        }

        // 예외처리 1-2) TST 3시간 미만 → 고정값 10
        if (tst < MIN_TST_MINUTES) {
            return SleepScoreResult(
                score = FIXED_LOW_SLEEP_SCORE,
                scoreDeep = null,
                scoreRem = null,
                scoreHrv = null,
                resolvedTst = tst,
                tstEstimatedFromInBed = tstEstimatedFromInBed,
                tstEstimatedFromBaseline = tstEstimatedFromBaseline,
                fixedLowSleep = true,
            )
        }

        // 2) DST·RST 결정: 실측 → 개인 평균 비율·TST 곱 → 기본비율·TST 곱
        val dstActual = input.deepMinutes?.toDouble()
        val rstActual = input.remMinutes?.toDouble()

        // 예외처리 2-1) DST·RST 둘 다 결측 + 개인 평균 비율도 없음 → 임시식 clamp((TST/420)*100, 10, 100)
        if (dstActual == null && rstActual == null && !deepBaselineOk && !remBaselineOk) {
            val score = ((tst / TST_REFERENCE_MINUTES) * 100.0).coerceIn(FIXED_LOW_SLEEP_SCORE, 100.0)
            return SleepScoreResult(
                score = score,
                scoreDeep = null, scoreRem = null, scoreHrv = null, resolvedTst = tst,
                tstEstimatedFromInBed = tstEstimatedFromInBed,
                tstEstimatedFromBaseline = tstEstimatedFromBaseline,
                deepEstimated = true, remEstimated = true,
            )
        }

        val deep = resolveStage(dstActual, baseline?.avgDeepRatio, DEFAULT_DEEP_RATIO, tst, deepBaselineOk)
        val rem = resolveStage(rstActual, baseline?.avgRemRatio, DEFAULT_REM_RATIO, tst, remBaselineOk)

        val scoreDeep = scoreDeep(deep.minutes, tst).coerceIn(0.0, 100.0)
        val scoreRem = scoreRem(rem.minutes, tst).coerceIn(0.0, 100.0)

        // 3) Score_HRV (NHRV/WHRV 결측 → HRV 항 제외 가중치)
        val whrv = input.weeklyHrvBaselineMs
        val nhrv = input.nightHrvMs
        val hrvMissing = nhrv == null || whrv == null || whrv == 0.0
        val scoreHrv: Double? = if (!hrvMissing) {
            val varHrv = nhrv!! / whrv!! * 100.0
            when {
                varHrv >= HRV_VAR_FULL -> 100.0
                varHrv >= HRV_VAR_FLOOR -> varHrv
                else -> 0.0
            }.coerceIn(0.0, 100.0)
        } else {
            null
        }

        // 4) 최종 합산
        val score = if (hrvMissing) {
            scoreDeep * DEEP_WEIGHT_NO_HRV + scoreRem * REM_WEIGHT_NO_HRV
        } else {
            scoreDeep * DEEP_WEIGHT + scoreRem * REM_WEIGHT + scoreHrv!! * HRV_WEIGHT
        }.coerceIn(0.0, 100.0)

        return SleepScoreResult(
            score = score,
            scoreDeep = scoreDeep,
            scoreRem = scoreRem,
            scoreHrv = scoreHrv,
            resolvedTst = tst,
            tstEstimatedFromInBed = tstEstimatedFromInBed,
            tstEstimatedFromBaseline = tstEstimatedFromBaseline,
            deepEstimated = deep.estimated,
            remEstimated = rem.estimated,
            deepFromBaseline = deep.fromBaseline,
            remFromBaseline = rem.fromBaseline,
            hrvMissing = hrvMissing,
        )
    }

    /** 실측 → 개인 평균 비율 → 기본비율 순으로 단계 분(minute) 추정. */
    private fun resolveStage(
        actual: Double?,
        baselineRatio: Double?,
        defaultRatio: Double,
        tst: Double,
        baselineOk: Boolean,
    ): StageEstimate = when {
        actual != null -> StageEstimate(actual, estimated = false, fromBaseline = false)
        baselineOk && baselineRatio != null -> StageEstimate(baselineRatio * tst, estimated = true, fromBaseline = true)
        else -> StageEstimate(defaultRatio * tst, estimated = true, fromBaseline = false)
    }

    private fun scoreDeep(dst: Double, tst: Double): Double {
        val ratioDeep = dst / tst * 100.0
        val base = if (ratioDeep >= DEEP_RATIO_FULL) 100.0 else ratioDeep * SUB_RATIO_MULTIPLIER
        return if (dst < DEEP_PENALTY_MINUTES) base * DEEP_PENALTY_FACTOR else base
    }

    private fun scoreRem(rst: Double, tst: Double): Double {
        val r = rst / tst * 100.0
        return when {
            r < REM_RATIO_LOW -> r * SUB_RATIO_MULTIPLIER
            r <= REM_RATIO_OPTIMAL_MAX -> 100.0
            r <= REM_RATIO_HIGH -> 100.0 - ((r - REM_RATIO_OPTIMAL_MAX) * REM_OVER_OPTIMAL_SLOPE)
            else -> 80.0 - ((r - REM_RATIO_HIGH) * REM_EXCESS_SLOPE)
        }
    }

    companion object {
        const val MIN_TST_MINUTES = 180.0
        const val FIXED_LOW_SLEEP_SCORE = 10.0
        const val TST_REFERENCE_MINUTES = 420.0
        const val IN_BED_TO_TST_RATIO = 0.85
        const val MIN_BASELINE_NIGHTS = 3 // 개인 평균 신뢰 최소 표본 수

        const val DEEP_RATIO_FULL = 20.0
        const val DEEP_PENALTY_MINUTES = 45.0
        const val DEEP_PENALTY_FACTOR = 0.8

        const val REM_RATIO_LOW = 20.0
        const val REM_RATIO_OPTIMAL_MAX = 25.0
        const val REM_RATIO_HIGH = 30.0
        const val REM_OVER_OPTIMAL_SLOPE = 4.0
        const val REM_EXCESS_SLOPE = 10.0

        const val SUB_RATIO_MULTIPLIER = 5.0

        const val HRV_VAR_FULL = 90.0
        const val HRV_VAR_FLOOR = 50.0

        const val DEEP_WEIGHT = 0.4
        const val REM_WEIGHT = 0.3
        const val HRV_WEIGHT = 0.3
        const val DEEP_WEIGHT_NO_HRV = 0.571
        const val REM_WEIGHT_NO_HRV = 0.429

        const val DEFAULT_DEEP_RATIO = 0.15
        const val DEFAULT_REM_RATIO = 0.22
    }
}
