package com.medilux.blt.domain.roi.service

import com.medilux.blt.domain.pvt.entity.PvtSession
import com.medilux.blt.domain.pvt.repository.PvtSessionRepository
import com.medilux.blt.domain.roi.dto.EvaluationCreateRequest
import com.medilux.blt.domain.roi.dto.EvaluationDetailResponse
import com.medilux.blt.domain.roi.dto.EvaluationPageResponse
import com.medilux.blt.domain.roi.dto.EvaluationResponse
import com.medilux.blt.domain.roi.dto.EvaluationStatsResponse
import com.medilux.blt.domain.roi.dto.EvaluationSummaryResponse
import com.medilux.blt.domain.roi.dto.HealthKitDataRequest
import com.medilux.blt.domain.roi.entity.BrainRoiScore
import com.medilux.blt.domain.roi.entity.Recommendation
import com.medilux.blt.domain.roi.repository.BrainRoiScoreRepository
import com.medilux.blt.domain.roi.repository.RecommendationRepository
import com.medilux.blt.domain.sleep.entity.SleepRecord
import com.medilux.blt.domain.sleep.repository.SleepRecordRepository
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.repository.UserRepository
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.LocalDate
import java.time.ZoneId
import java.util.Base64

@Service
class EvaluationService(
    private val userRepository: UserRepository,
    private val sleepRecordRepository: SleepRecordRepository,
    private val pvtSessionRepository: PvtSessionRepository,
    private val brainRoiScoreRepository: BrainRoiScoreRepository,
    private val recommendationRepository: RecommendationRepository,
    private val brainRoiCalculator: BrainRoiCalculator,
    private val recommendationProvider: RecommendationProvider,
) {
    /**
     * ROI 평가 제출 — 점수 계산 + 추천 생성 후 append 저장.
     * 같은 sleepDate의 SleepRecord는 재사용/갱신(수면 일자 공유), BrainRoiScore는 항상 새로 추가.
     */
    @Transactional
    fun submit(userId: Long, request: EvaluationCreateRequest): EvaluationResponse {
        validatePvt(request)
        validateSleep(request.healthKitData)

        val user = userRepository.findById(userId).orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }
        val zone = resolveZone(request.timezone)
        val sleepDate = request.healthKitData?.sleepDate ?: LocalDate.ofInstant(request.evaluatedAt, zone)

        val (sleepRecord, sleepDataPresent) = resolveSleepRecord(user, sleepDate, request.healthKitData)
        val session = pvtSessionRepository.save(toPvtSession(user, request))

        // PVT_ONLY(합성 최소 레코드)는 baseline 추정을 적용하지 않음(예외식1 유지). 실제 수면 데이터가 있을 때만 보강.
        val baseline = if (sleepDataPresent) personalBaseline(userId, sleepDate) else null
        val result = brainRoiCalculator.calculate(
            avgRtMs = request.pvt.avgRtMs,
            sleepDataPresent = sleepDataPresent,
            sleepInput = sleepRecord.toScoreInput(baseline),
        )

        val score = brainRoiScoreRepository.save(
            BrainRoiScore(
                user = user,
                session = session,
                sleepRecord = sleepRecord,
                calculationScenario = result.scenario,
                finalScore = result.finalScore,
                sleepScore = result.sleepScore,
                pvtScore = result.pvtScore,
                quadrant = result.quadrant,
                formulaVersion = result.formulaVersion,
                breakdown = result.breakdown,
                measuredAt = request.evaluatedAt,
            ),
        )

        val recommendationContext = RecommendationContext(score = score, sleep = sleepRecord, session = session, user = user)
        val recommendations = recommendationRepository.saveAll(recommendationProvider.generate(recommendationContext))
        val trend = brainRoiScoreRepository
            .findFirstByUserIdAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(userId, request.evaluatedAt)
            ?.let { score.finalScore - it.finalScore }

        return EvaluationResponse.from(score, recommendations, trend)
    }

    /** 오늘 최신 1건. 없으면 null(컨트롤러에서 204). 타임존은 사용자 설정 기준. */
    @Transactional(readOnly = true)
    fun getToday(userId: Long): EvaluationResponse? {
        val zone = resolveZone(userZone(userId))
        val today = LocalDate.now(zone)
        val start = today.atStartOfDay(zone).toInstant()
        val end = today.plusDays(1).atStartOfDay(zone).toInstant()
        val score = brainRoiScoreRepository
            .findFirstByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(userId, start, end)
            ?: return null
        return EvaluationResponse.from(score, recommendationsOf(score), trendOf(score))
    }

    /** 상세 — 수면·PVT 카드 지표 전개. */
    @Transactional(readOnly = true)
    fun getDetail(userId: Long, evaluationId: Long): EvaluationDetailResponse {
        val score = brainRoiScoreRepository.findByIdAndUserId(evaluationId, userId)
            ?: throw BltException(ErrorCode.BRAIN_ROI_NOT_FOUND)
        // PVT_ONLY로 생성된 최소 SleepRecord(totalMinutes=0)는 수면 카드에서 숨긴다.
        val sleep = score.sleepRecord.takeIf { it.totalMinutes > 0 }
        return EvaluationDetailResponse.from(score, recommendationsOf(score), trendOf(score), sleep, score.session)
    }

    /** History 목록 — 기간 + 커서. 같은 날 복수 측정도 개별 행으로 반환. */
    @Transactional(readOnly = true)
    fun list(userId: Long, from: LocalDate?, to: LocalDate?, cursor: String?, size: Int): EvaluationPageResponse {
        val zone = resolveZone(userZone(userId))
        val pageSize = size.coerceIn(MIN_PAGE_SIZE, MAX_PAGE_SIZE)
        val today = LocalDate.now(zone)
        val fromDate = from ?: today.withDayOfMonth(1)
        val toDate = to ?: today
        val start = fromDate.atStartOfDay(zone).toInstant()
        val end = toDate.plusDays(1).atStartOfDay(zone).toInstant()
        val cursorId = decodeCursor(cursor)

        val fetched = brainRoiScoreRepository.findPage(userId, start, end, cursorId, PageRequest.of(0, pageSize + 1))
        val hasNext = fetched.size > pageSize
        val items = fetched.take(pageSize)
        val nextCursor = if (hasNext) encodeCursor(items.last().id) else null

        return EvaluationPageResponse(
            items = items.map { EvaluationSummaryResponse.from(it, LocalDate.ofInstant(it.measuredAt, zone)) },
            nextCursor = nextCursor,
            hasNext = hasNext,
        )
    }

    /** 통계 — 측정 일수/평균/최고/최저 ROI. */
    @Transactional(readOnly = true)
    fun stats(userId: Long, period: String, from: LocalDate?, to: LocalDate?): EvaluationStatsResponse {
        val zone = resolveZone(userZone(userId))
        val today = LocalDate.now(zone)
        val fromDate = from ?: today.withDayOfMonth(1)
        val toDate = to ?: today
        val start = fromDate.atStartOfDay(zone).toInstant()
        val end = toDate.plusDays(1).atStartOfDay(zone).toInstant()

        val rows = brainRoiScoreRepository.findScoresInRange(userId, start, end)
        val measuredDays = rows.map { LocalDate.ofInstant(it.measuredAt, zone) }.distinct().size
        val scores = rows.map { it.finalScore }

        return EvaluationStatsResponse(
            period = period,
            from = fromDate,
            to = toDate,
            measuredDays = measuredDays,
            avgRoi = scores.average().takeIf { scores.isNotEmpty() }?.let(Math::round)?.toInt(),
            maxRoi = scores.maxOrNull(),
            minRoi = scores.minOrNull(),
        )
    }

    private fun recommendationsOf(score: BrainRoiScore): List<Recommendation> =
        recommendationRepository.findByRoiScoreIdOrderByIdAsc(score.id)

    private fun trendOf(score: BrainRoiScore): Int? = brainRoiScoreRepository
        .findFirstByUserIdAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(score.user.id, score.measuredAt)
        ?.let { score.finalScore - it.finalScore }

    private fun userZone(userId: Long): String = userRepository.findById(userId)
        .orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }
        .notificationTimezone

    private fun encodeCursor(id: Long): String = Base64.getUrlEncoder().withoutPadding().encodeToString(id.toString().toByteArray())

    private fun decodeCursor(cursor: String?): Long? = cursor
        ?.let { runCatching { String(Base64.getUrlDecoder().decode(it)).toLong() }.getOrNull() }

    private fun validatePvt(request: EvaluationCreateRequest) {
        val pvt = request.pvt
        if (!pvt.isValid || pvt.rawRtMs.isEmpty() || pvt.totalCount <= 0 || pvt.avgRtMs <= 0) {
            throw BltException(ErrorCode.INVALID_PVT)
        }
    }

    private fun validateSleep(hk: HealthKitDataRequest?) {
        if (hk == null) return
        val minutes = listOfNotNull(
            hk.totalMinutes,
            hk.deepMinutes,
            hk.remMinutes,
            hk.coreMinutes,
            hk.awakeMinutes,
            hk.inBedMinutes,
            hk.unspecifiedMinutes,
        )
        if (minutes.any { it < 0 }) {
            throw BltException(ErrorCode.INVALID_SLEEP_DATA)
        }
    }

    /** 그날 SleepRecord 재사용/갱신/생성. PVT_ONLY는 기존 기록 재사용, 없으면 최소 레코드 생성. */
    private fun resolveSleepRecord(user: User, sleepDate: LocalDate, hk: HealthKitDataRequest?): Pair<SleepRecord, Boolean> {
        val existing = sleepRecordRepository.findFirstByUserIdAndSleepDateOrderByIdDesc(user.id, sleepDate)
        return when {
            hk != null && existing != null -> {
                existing.applyHealthKit(hk)
                existing to true
            }
            hk != null -> sleepRecordRepository.save(hk.toSleepRecord(user)) to true
            existing != null -> existing to true
            else -> sleepRecordRepository.save(minimalSleepRecord(user, sleepDate)) to false
        }
    }

    /** 최근 7일(당일 제외) 유효 야간으로 개인 baseline 계산. 데이터 없으면 null → 결측 시 기본비율 폴백. */
    private fun personalBaseline(userId: Long, sleepDate: LocalDate): SleepPersonalBaseline? {
        val recent = sleepRecordRepository.findByUserIdAndSleepDateBetweenAndTotalMinutesGreaterThan(
            userId,
            sleepDate.minusDays(BASELINE_WINDOW_DAYS),
            sleepDate.minusDays(1),
            0,
        )
        if (recent.isEmpty()) return null

        val tsts = recent.map { it.totalMinutes.toDouble() }
        val deepRatios = recent.mapNotNull { r -> r.deepMinutes?.let { it.toDouble() / r.totalMinutes } }
        val remRatios = recent.mapNotNull { r -> r.remMinutes?.let { it.toDouble() / r.totalMinutes } }

        return SleepPersonalBaseline(
            avgTstMinutes = tsts.average().takeIf { tsts.isNotEmpty() },
            avgDeepRatio = deepRatios.average().takeIf { deepRatios.isNotEmpty() },
            avgRemRatio = remRatios.average().takeIf { remRatios.isNotEmpty() },
            tstNights = tsts.size,
            deepNights = deepRatios.size,
            remNights = remRatios.size,
        )
    }

    private fun resolveZone(timezone: String): ZoneId = runCatching { ZoneId.of(timezone) }
        .getOrElse { ZoneId.of(DEFAULT_ZONE) }

    private fun minimalSleepRecord(user: User, sleepDate: LocalDate): SleepRecord =
        SleepRecord(user = user, sleepDate = sleepDate, totalMinutes = 0)

    private fun HealthKitDataRequest.toSleepRecord(user: User): SleepRecord = SleepRecord(
        user = user,
        sleepDate = sleepDate,
        totalMinutes = totalMinutes,
        deepMinutes = deepMinutes,
        remMinutes = remMinutes,
        coreMinutes = coreMinutes,
        awakeMinutes = awakeMinutes,
        inBedMinutes = inBedMinutes,
        unspecifiedMinutes = unspecifiedMinutes,
        sampleCount = sampleCount,
        nightHrvMs = nightHrvMs,
        weeklyHrvBaselineMs = weeklyHrvBaselineMs,
        dataCompleteness = dataCompleteness,
        rawPayload = rawPayload,
    )

    private fun SleepRecord.applyHealthKit(hk: HealthKitDataRequest) {
        totalMinutes = hk.totalMinutes
        deepMinutes = hk.deepMinutes
        remMinutes = hk.remMinutes
        coreMinutes = hk.coreMinutes
        awakeMinutes = hk.awakeMinutes
        inBedMinutes = hk.inBedMinutes
        unspecifiedMinutes = hk.unspecifiedMinutes
        sampleCount = hk.sampleCount
        nightHrvMs = hk.nightHrvMs
        weeklyHrvBaselineMs = hk.weeklyHrvBaselineMs
        dataCompleteness = hk.dataCompleteness
        if (hk.rawPayload != null) rawPayload = hk.rawPayload
    }

    private fun SleepRecord.toScoreInput(baseline: SleepPersonalBaseline?): SleepScoreInput = SleepScoreInput(
        totalMinutes = totalMinutes,
        deepMinutes = deepMinutes,
        remMinutes = remMinutes,
        nightHrvMs = nightHrvMs,
        weeklyHrvBaselineMs = weeklyHrvBaselineMs,
        inBedMinutes = inBedMinutes,
        baseline = baseline,
    )

    private fun toPvtSession(user: User, request: EvaluationCreateRequest): PvtSession {
        val pvt = request.pvt
        return PvtSession(
            user = user,
            measurementId = pvt.measurementId,
            startedAt = pvt.startedAt,
            endedAt = pvt.endedAt,
            totalDurationMs = pvt.totalDurationMs,
            totalCount = pvt.totalCount,
            rawRtMs = pvt.rawRtMs,
            avgRtMs = pvt.avgRtMs,
            medianRtMs = pvt.medianRtMs,
            lapsesMild = pvt.lapsesMild,
            lapsesTimeout = pvt.lapsesTimeout,
            falseStarts = pvt.falseStarts,
            isValid = pvt.isValid,
            invalidReason = pvt.invalidReason,
            trials = pvt.trials,
        )
    }

    companion object {
        private const val DEFAULT_ZONE = "Asia/Seoul"
        private const val BASELINE_WINDOW_DAYS = 7L
        private const val MIN_PAGE_SIZE = 1
        private const val MAX_PAGE_SIZE = 100
    }
}
