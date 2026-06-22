package com.medilux.blt.domain.roi.service

import com.medilux.blt.domain.roi.entity.CalculationScenario
import com.medilux.blt.domain.roi.entity.RoiQuadrant
import org.springframework.stereotype.Component
import kotlin.math.roundToInt

/**
 * Brain ROI 최종 산출기 — Score_PVT, Score_Sleep 이용
 * Score_Final·quadrant·scenario·breakdown 결정
 */
@Component
class BrainRoiCalculator(private val pvtScoreCalculator: PvtScoreCalculator, private val sleepScoreCalculator: SleepScoreCalculator) {
    /**
     * @param sleepDataPresent healthKitData(수면 데이터)가 제출됐는지. false면 PVT_ONLY(시나리오 D).
     * @param sleepInput 수면 입력. PVT_ONLY여도 빈 입력을 넘겨 산출 결과(전부 null)를 통일되게 받는다.
     */
    fun calculate(avgRtMs: Double, sleepDataPresent: Boolean, sleepInput: SleepScoreInput): BrainRoiResult {
        val pvt = pvtScoreCalculator.calculate(avgRtMs)
        val sleepResult = sleepScoreCalculator.calculate(sleepInput)
        val sleepScore = sleepResult.score

        val finalDouble = if (sleepScore == null) {
            (pvt * pvt / 100.0) * EXCEPTION1_SQUARE_WEIGHT + pvt * EXCEPTION1_LINEAR_WEIGHT
        } else {
            sleepScore * SLEEP_WEIGHT + pvt * PVT_WEIGHT
        }
        val finalScore = finalDouble.coerceIn(0.0, 100.0).roundToInt()
        val pvtScore = pvt.roundToInt()
        val storedSleepScore = sleepScore?.roundToInt() ?: 0

        val scenario = resolveScenario(sleepDataPresent, sleepResult)
        val quadrant = resolveQuadrant(storedSleepScore, pvtScore)
        val breakdown = buildBreakdown(avgRtMs, pvtScore, sleepScore, sleepResult)

        return BrainRoiResult(
            finalScore = finalScore,
            sleepScore = storedSleepScore,
            pvtScore = pvtScore,
            sleepScoreNullable = sleepScore?.roundToInt(),
            scenario = scenario,
            quadrant = quadrant,
            formulaVersion = FORMULA_VERSION,
            breakdown = breakdown,
        )
    }

    private fun resolveScenario(sleepDataPresent: Boolean, sleep: SleepScoreResult): CalculationScenario = when {
        !sleepDataPresent -> CalculationScenario.D

        // 수면 데이터는 왔으나 산출 불가(케이스 5 등)
        sleep.score == null -> CalculationScenario.C

        // 결측 보정(단계 추정 또는 TST 추정[inBed·baseline])은 모두 C로 일관 분류
        sleep.deepEstimated ||
            sleep.remEstimated ||
            sleep.tstEstimatedFromInBed ||
            sleep.tstEstimatedFromBaseline -> CalculationScenario.C

        sleep.hrvMissing -> CalculationScenario.B

        else -> CalculationScenario.A
    }

    private fun resolveQuadrant(sleepScore: Int, pvtScore: Int): RoiQuadrant {
        val sleepHigh = sleepScore >= HIGH_AXIS_THRESHOLD
        val pvtHigh = pvtScore >= HIGH_AXIS_THRESHOLD
        return when {
            sleepHigh && pvtHigh -> RoiQuadrant.HIGH_SLEEP_HIGH_PVT
            sleepHigh && !pvtHigh -> RoiQuadrant.HIGH_SLEEP_LOW_PVT
            !sleepHigh && pvtHigh -> RoiQuadrant.LOW_SLEEP_HIGH_PVT
            else -> RoiQuadrant.LOW_SLEEP_LOW_PVT
        }
    }

    private fun buildBreakdown(avgRtMs: Double, pvtScore: Int, sleepScore: Double?, sleep: SleepScoreResult): Map<String, Any?> = mapOf(
        "sleepWeight" to if (sleepScore == null) null else SLEEP_WEIGHT,
        "pvtWeight" to if (sleepScore == null) null else PVT_WEIGHT,
        "finalFormula" to if (sleepScore == null) "PVT_ONLY_EXCEPTION1" else "WEIGHTED",
        "scoreSleep" to sleepScore?.roundToInt(),
        "scorePvt" to pvtScore,
        "scoreDeep" to sleep.scoreDeep?.roundToInt(),
        "scoreRem" to sleep.scoreRem?.roundToInt(),
        "scoreHrv" to sleep.scoreHrv?.roundToInt(),
        "avgRtMs" to avgRtMs,
        "resolvedTstMinutes" to sleep.resolvedTst?.roundToInt(),
        "sleepMissing" to (sleepScore == null),
        "tstEstimatedFromInBed" to sleep.tstEstimatedFromInBed,
        "tstEstimatedFromBaseline" to sleep.tstEstimatedFromBaseline,
        "deepEstimated" to sleep.deepEstimated,
        "deepFromBaseline" to sleep.deepFromBaseline,
        "remEstimated" to sleep.remEstimated,
        "remFromBaseline" to sleep.remFromBaseline,
        "hrvMissing" to sleep.hrvMissing,
        "fixedLowSleep" to sleep.fixedLowSleep,
    )

    companion object {
        const val FORMULA_VERSION = "v2"

        const val SLEEP_WEIGHT = 0.4
        const val PVT_WEIGHT = 0.6
        const val EXCEPTION1_SQUARE_WEIGHT = 0.2
        const val EXCEPTION1_LINEAR_WEIGHT = 0.8

        const val HIGH_AXIS_THRESHOLD = 65
    }
}

/** Brain ROI 산출 결과 — 영속화·응답 양쪽에서 재사용. */
data class BrainRoiResult(
    val finalScore: Int,
    val sleepScore: Int,
    val pvtScore: Int,
    /** Score_Sleep 산출 불가 시 null(예외식1 적용). 저장용 [sleepScore]는 0으로 대체됨. */
    val sleepScoreNullable: Int?,
    val scenario: CalculationScenario,
    val quadrant: RoiQuadrant,
    val formulaVersion: String,
    val breakdown: Map<String, Any?>,
)
