package com.medilux.blt.domain.roi.dto

import com.medilux.blt.domain.pvt.entity.PvtSession
import com.medilux.blt.domain.roi.entity.BrainRoiScore
import com.medilux.blt.domain.roi.entity.CalculationScenario
import com.medilux.blt.domain.roi.entity.Recommendation
import com.medilux.blt.domain.roi.entity.RoiQuadrant
import com.medilux.blt.domain.sleep.entity.SleepDataCompleteness
import com.medilux.blt.domain.sleep.entity.SleepRecord
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant
import java.time.LocalDate
import java.util.UUID
import kotlin.math.roundToInt

// ---------------------------------------------------------------------------
// Request
// ---------------------------------------------------------------------------

@Schema(description = "ROI 평가 제출 요청 (PVT 필수 + 수면 데이터 선택)")
data class EvaluationCreateRequest(
    @field:Schema(description = "측정/평가 시각", example = "2026-05-30T09:43:00Z")
    val evaluatedAt: Instant,
    @field:Schema(description = "타임존 (수면 귀속일·today 판정)", example = "Asia/Seoul")
    val timezone: String,
    @field:Schema(description = "수면/HRV 데이터. 없으면 PVT_ONLY")
    val healthKitData: HealthKitDataRequest? = null,
    @field:Schema(description = "PVT 측정 결과")
    val pvt: PvtRequest,
)

@Schema(description = "HealthKit 수면 데이터 (= SleepRecord)")
data class HealthKitDataRequest(
    val sleepDate: LocalDate,
    val totalMinutes: Int,
    val deepMinutes: Int? = null,
    val remMinutes: Int? = null,
    val coreMinutes: Int? = null,
    val awakeMinutes: Int? = null,
    val inBedMinutes: Int? = null,
    val unspecifiedMinutes: Int? = null,
    val sampleCount: Int? = null,
    val nightHrvMs: Double? = null,
    val weeklyHrvBaselineMs: Double? = null,
    val dataCompleteness: SleepDataCompleteness? = null,
    val rawPayload: Map<String, Any?>? = null,
)

@Schema(description = "PVT 측정 결과 (= PvtSession)")
data class PvtRequest(
    val measurementId: UUID,
    val startedAt: Instant,
    val endedAt: Instant,
    val totalDurationMs: Int,
    val totalCount: Int,
    val rawRtMs: List<Int>,
    val avgRtMs: Double,
    val medianRtMs: Double? = null,
    val lapsesMild: Int,
    val lapsesTimeout: Int,
    val falseStarts: Int,
    val isValid: Boolean,
    val invalidReason: String? = null,
    val trials: List<Map<String, Any?>> = emptyList(),
)

// ---------------------------------------------------------------------------
// Response — 점수 + 추천
// ---------------------------------------------------------------------------

@Schema(description = "ROI 평가 결과 (점수 + 추천)")
data class EvaluationResponse(
    val evaluationId: Long,
    val finalScore: Int,
    val sleepScore: Int,
    val pvtScore: Int,
    val quadrant: RoiQuadrant,
    val calculationScenario: CalculationScenario,
    @field:Schema(description = "상태 라벨 (최적 집중/안정적/주의/회복 필요)")
    val statusLabel: String,
    val formulaVersion: String,
    @field:Schema(description = "직전 측정 대비 변화(점). 없으면 null")
    val trendVsYesterday: Int?,
    val measuredAt: Instant,
    val breakdown: Map<String, Any?>,
    val recommendations: List<RecommendationResponse>,
) {
    companion object {
        fun from(score: BrainRoiScore, recommendations: List<Recommendation>, trendVsYesterday: Int?): EvaluationResponse =
            EvaluationResponse(
                evaluationId = score.id,
                finalScore = score.finalScore,
                sleepScore = score.sleepScore,
                pvtScore = score.pvtScore,
                quadrant = score.quadrant,
                calculationScenario = score.calculationScenario,
                statusLabel = RoiStatusLabel.of(score.finalScore),
                formulaVersion = score.formulaVersion,
                trendVsYesterday = trendVsYesterday,
                measuredAt = score.measuredAt,
                breakdown = score.breakdown,
                recommendations = recommendations.map(RecommendationResponse::from),
            )
    }
}

@Schema(description = "추천 카드")
data class RecommendationResponse(
    val recommendationId: Long,
    val quadrantKey: String,
    val title: String,
    val message: String,
    val suggestedTasks: List<Map<String, Any?>>,
) {
    companion object {
        fun from(rec: Recommendation): RecommendationResponse = RecommendationResponse(
            recommendationId = rec.id,
            quadrantKey = rec.quadrantKey,
            title = rec.title,
            message = rec.message,
            suggestedTasks = rec.suggestedTasks,
        )
    }
}

/** finalScore → 상태 라벨 (80+/65~79/50~64/~49 4단계). */
object RoiStatusLabel {
    fun of(finalScore: Int): String = when {
        finalScore >= 80 -> "최적 집중"
        finalScore >= 65 -> "안정적"
        finalScore >= 50 -> "주의"
        else -> "회복 필요"
    }
}

// ---------------------------------------------------------------------------
// Response — 상세 (수면 카드 / PVT 카드 파생 지표)
// ---------------------------------------------------------------------------

@Schema(description = "ROI 평가 상세 (수면·PVT 카드 포함)")
data class EvaluationDetailResponse(val evaluation: EvaluationResponse, val sleep: SleepDetail?, val pvt: PvtDetail) {
    companion object {
        fun from(
            score: BrainRoiScore,
            recommendations: List<Recommendation>,
            trendVsYesterday: Int?,
            sleep: SleepRecord?,
            pvt: PvtSession,
        ): EvaluationDetailResponse = EvaluationDetailResponse(
            evaluation = EvaluationResponse.from(score, recommendations, trendVsYesterday),
            sleep = sleep?.let(SleepDetail::from),
            pvt = PvtDetail.from(pvt),
        )
    }
}

@Schema(description = "수면 상세 지표 (일부는 파생 계산)")
data class SleepDetail(
    val sleepDate: LocalDate,
    val totalMinutes: Int,
    val deepMinutes: Int?,
    val remMinutes: Int?,
    val coreMinutes: Int?,
    val awakeMinutes: Int?,
    val inBedMinutes: Int?,
    val efficiencyPercent: Int?,
    val deepRatioPercent: Int?,
    val remRatioPercent: Int?,
    val lightRatioPercent: Int?,
    val dataCompleteness: SleepDataCompleteness?,
) {
    companion object {
        fun from(s: SleepRecord): SleepDetail {
            // 입력 이상치(예: 단계 합 > TST, TST > inBed)에서도 표시 지표는 0~100으로 clamp.
            fun ratio(part: Int?): Int? = if (part != null && s.totalMinutes > 0) {
                (part.toDouble() / s.totalMinutes * 100).roundToInt().coerceIn(0, 100)
            } else {
                null
            }

            val efficiency = s.inBedMinutes
                ?.takeIf { it > 0 }
                ?.let { (s.totalMinutes.toDouble() / it * 100).roundToInt().coerceIn(0, 100) }
            return SleepDetail(
                sleepDate = s.sleepDate,
                totalMinutes = s.totalMinutes,
                deepMinutes = s.deepMinutes,
                remMinutes = s.remMinutes,
                coreMinutes = s.coreMinutes,
                awakeMinutes = s.awakeMinutes,
                inBedMinutes = s.inBedMinutes,
                efficiencyPercent = efficiency,
                deepRatioPercent = ratio(s.deepMinutes),
                remRatioPercent = ratio(s.remMinutes),
                lightRatioPercent = ratio(s.coreMinutes),
                dataCompleteness = s.dataCompleteness,
            )
        }
    }
}

@Schema(description = "PVT 상세 지표 (일부는 파생 계산)")
data class PvtDetail(
    val measurementId: UUID,
    val avgRtMs: Double,
    val bestRtMs: Int?,
    val medianRtMs: Double?,
    val lapsesMild: Int,
    val lapsesTimeout: Int,
    val falseStarts: Int,
    val totalCount: Int,
    val rawRtMs: List<Int>,
    val isValid: Boolean,
) {
    companion object {
        fun from(p: PvtSession): PvtDetail = PvtDetail(
            measurementId = p.measurementId,
            avgRtMs = p.avgRtMs,
            bestRtMs = p.rawRtMs.minOrNull(),
            medianRtMs = p.medianRtMs,
            lapsesMild = p.lapsesMild,
            lapsesTimeout = p.lapsesTimeout,
            falseStarts = p.falseStarts,
            totalCount = p.totalCount,
            rawRtMs = p.rawRtMs,
            isValid = p.isValid,
        )
    }
}

// ---------------------------------------------------------------------------
// Response — 목록(History) / 통계
// ---------------------------------------------------------------------------

@Schema(description = "History 캘린더 셀용 경량 요약")
data class EvaluationSummaryResponse(
    val evaluationId: Long,
    val date: LocalDate,
    val measuredAt: Instant,
    val finalScore: Int,
    val statusLabel: String,
    val quadrant: RoiQuadrant,
) {
    companion object {
        fun from(score: BrainRoiScore, date: LocalDate): EvaluationSummaryResponse = EvaluationSummaryResponse(
            evaluationId = score.id,
            date = date,
            measuredAt = score.measuredAt,
            finalScore = score.finalScore,
            statusLabel = RoiStatusLabel.of(score.finalScore),
            quadrant = score.quadrant,
        )
    }
}

@Schema(description = "ROI 평가 목록 (커서 페이지네이션)")
data class EvaluationPageResponse(val items: List<EvaluationSummaryResponse>, val nextCursor: String?, val hasNext: Boolean)

@Schema(description = "ROI 평가 통계")
data class EvaluationStatsResponse(
    val period: String,
    val from: LocalDate,
    val to: LocalDate,
    val measuredDays: Int,
    val avgRoi: Int?,
    val maxRoi: Int?,
    val minRoi: Int?,
)
