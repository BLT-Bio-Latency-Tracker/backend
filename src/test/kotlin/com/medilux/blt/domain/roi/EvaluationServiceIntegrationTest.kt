package com.medilux.blt.domain.roi

import com.medilux.blt.domain.roi.dto.EvaluationCreateRequest
import com.medilux.blt.domain.roi.dto.HealthKitDataRequest
import com.medilux.blt.domain.roi.dto.PvtRequest
import com.medilux.blt.domain.roi.entity.CalculationScenario
import com.medilux.blt.domain.roi.repository.BrainRoiScoreRepository
import com.medilux.blt.domain.roi.repository.RecommendationRepository
import com.medilux.blt.domain.roi.service.BrainRoiCalculator
import com.medilux.blt.domain.roi.service.EvaluationService
import com.medilux.blt.domain.roi.service.PvtScoreCalculator
import com.medilux.blt.domain.roi.service.RuleBasedRecommendationProvider
import com.medilux.blt.domain.roi.service.SleepScoreCalculator
import com.medilux.blt.domain.sleep.repository.SleepRecordRepository
import com.medilux.blt.domain.user.entity.AuthType
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.global.exception.BltException
import org.assertj.core.api.Assertions.assertThat
import org.assertj.core.api.Assertions.assertThatThrownBy
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.context.annotation.Import
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.LocalDate
import java.time.temporal.ChronoUnit
import java.util.UUID

@Testcontainers
@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@Import(
    EvaluationService::class,
    BrainRoiCalculator::class,
    PvtScoreCalculator::class,
    SleepScoreCalculator::class,
    RuleBasedRecommendationProvider::class,
)
class EvaluationServiceIntegrationTest {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var evaluationService: EvaluationService

    @Autowired
    lateinit var brainRoiScoreRepository: BrainRoiScoreRepository

    @Autowired
    lateinit var sleepRecordRepository: SleepRecordRepository

    @Autowired
    lateinit var recommendationRepository: RecommendationRepository

    private fun persistUser(hash: String = "hash-roi"): User =
        entityManager.persistAndFlush(User(appleSubHash = hash, authType = AuthType.APPLE))

    private fun pvt(avgRtMs: Double = 312.0, isValid: Boolean = true) = PvtRequest(
        measurementId = UUID.randomUUID(),
        startedAt = Instant.now().minusSeconds(30),
        endedAt = Instant.now(),
        totalDurationMs = 30000,
        totalCount = 7,
        rawRtMs = listOf(250, 310, 520, 400, 290, 360, 305),
        avgRtMs = avgRtMs,
        medianRtMs = 310.0,
        lapsesMild = 1,
        lapsesTimeout = 0,
        falseStarts = 1,
        isValid = isValid,
    )

    private fun healthKit(sleepDate: LocalDate = LocalDate.now(), deepMinutes: Int? = 72, remMinutes: Int? = 88) = HealthKitDataRequest(
        sleepDate = sleepDate,
        totalMinutes = 400,
        deepMinutes = deepMinutes,
        remMinutes = remMinutes,
        coreMinutes = 220,
        awakeMinutes = 20,
        inBedMinutes = 460,
        nightHrvMs = 45.2,
        weeklyHrvBaselineMs = 48.1,
    )

    private fun request(
        evaluatedAt: Instant = Instant.now(),
        healthKitData: HealthKitDataRequest? = healthKit(),
        pvt: PvtRequest = pvt(),
    ) = EvaluationCreateRequest(
        evaluatedAt = evaluatedAt,
        timezone = "Asia/Seoul",
        healthKitData = healthKitData,
        pvt = pvt,
    )

    @Test
    fun `submit with full data persists score, recommendation, and sleep record`() {
        val user = persistUser()

        val response = evaluationService.submit(user.id, request())

        assertThat(response.calculationScenario).isEqualTo(CalculationScenario.A)
        assertThat(response.finalScore).isEqualTo(95)
        assertThat(response.recommendations).hasSize(1)
        assertThat(brainRoiScoreRepository.findById(response.evaluationId)).isPresent
        assertThat(recommendationRepository.findByRoiScoreIdOrderByIdAsc(response.evaluationId)).hasSize(1)
    }

    @Test
    fun `PVT_ONLY submit uses scenario D and creates minimal hidden sleep record`() {
        val user = persistUser()

        val response = evaluationService.submit(user.id, request(healthKitData = null))

        assertThat(response.calculationScenario).isEqualTo(CalculationScenario.D)
        assertThat(response.sleepScore).isEqualTo(0)
        assertThat(response.breakdown["sleepMissing"]).isEqualTo(true)

        val detail = evaluationService.getDetail(user.id, response.evaluationId)
        assertThat(detail.sleep).isNull() // 최소 레코드는 수면 카드에서 숨김
    }

    @Test
    fun `same sleepDate reuses one sleep record but appends scores`() {
        val user = persistUser()
        val today = LocalDate.now()

        evaluationService.submit(user.id, request())
        evaluationService.submit(user.id, request(evaluatedAt = Instant.now().plusSeconds(1)))

        assertThat(sleepRecordRepository.findFirstByUserIdAndSleepDateOrderByIdDesc(user.id, today)).isNotNull
        assertThat(sleepRecordRepository.findAll().filter { it.user.id == user.id }).hasSize(1)
        assertThat(brainRoiScoreRepository.findScoresInRange(user.id, Instant.EPOCH, Instant.now().plusSeconds(60)))
            .hasSize(2)
    }

    @Test
    fun `missing stage uses 7-day personal baseline ratio`() {
        val user = persistUser()
        val today = LocalDate.now()
        // 최근 3일 deep ratio 0.20(80/400) 기록 → baseline 신뢰 충족
        listOf(1L, 2L, 3L).forEach { ago ->
            evaluationService.submit(
                user.id,
                request(healthKitData = healthKit(sleepDate = today.minusDays(ago), deepMinutes = 80)),
            )
        }

        // 오늘 deep 결측 → baseline 0.20 * 400 = 80분 → ratio 20 → scoreDeep 100 (기본비율이라면 75)
        val response = evaluationService.submit(user.id, request(healthKitData = healthKit(deepMinutes = null)))

        assertThat(response.breakdown["deepEstimated"]).isEqualTo(true)
        assertThat(response.breakdown["scoreDeep"]).isEqualTo(100)
    }

    @Test
    fun `invalid pvt is rejected`() {
        val user = persistUser()
        assertThatThrownBy { evaluationService.submit(user.id, request(pvt = pvt(isValid = false))) }
            .isInstanceOf(BltException::class.java)
    }

    @Test
    fun `getToday returns latest of today and stats aggregates`() {
        val user = persistUser()
        evaluationService.submit(user.id, request(pvt = pvt(avgRtMs = 450.0)))
        val latest = evaluationService.submit(user.id, request(evaluatedAt = Instant.now().plusSeconds(2)))

        val today = evaluationService.getToday(user.id)
        assertThat(today).isNotNull
        assertThat(today!!.evaluationId).isEqualTo(latest.evaluationId)
        assertThat(today.trendVsYesterday).isNotNull

        val stats = evaluationService.stats(user.id, "month", null, null)
        assertThat(stats.measuredDays).isEqualTo(1)
        assertThat(stats.maxRoi).isGreaterThanOrEqualTo(stats.minRoi!!)
    }

    @Test
    fun `getToday returns null when no measurement today`() {
        val user = persistUser()
        val tenDaysAgo = Instant.now().minus(10, ChronoUnit.DAYS)
        evaluationService.submit(
            user.id,
            request(evaluatedAt = tenDaysAgo, healthKitData = healthKit(LocalDate.now().minusDays(10))),
        )

        assertThat(evaluationService.getToday(user.id)).isNull()
    }

    @Test
    fun `list paginates with cursor`() {
        val user = persistUser()
        repeat(3) { i ->
            evaluationService.submit(user.id, request(evaluatedAt = Instant.now().plusSeconds(i.toLong())))
        }

        val first = evaluationService.list(user.id, null, null, null, 2)
        assertThat(first.items).hasSize(2)
        assertThat(first.hasNext).isTrue()

        val second = evaluationService.list(user.id, null, null, first.nextCursor, 2)
        assertThat(second.items).hasSize(1)
        assertThat(second.hasNext).isFalse()
    }

    @Test
    fun `list pagination keeps all rows when measuredAt and id order diverge (composite cursor)`() {
        val user = persistUser()
        val now = Instant.now()
        // 지연 업로드: id 순서(1,2,3)와 measuredAt 순서(now > now-50 > now-100)가 어긋나도록 삽입
        evaluationService.submit(user.id, request(evaluatedAt = now)) // id1 / now
        evaluationService.submit(user.id, request(evaluatedAt = now.minusSeconds(100))) // id2 / 가장 과거
        evaluationService.submit(user.id, request(evaluatedAt = now.minusSeconds(50))) // id3 / 중간 (최신 id, 중간 시각)

        val collected = mutableListOf<Long>()
        var cursor: String? = null
        do {
            val page = evaluationService.list(user.id, null, null, cursor, 1)
            collected += page.items.map { it.evaluationId }
            cursor = page.nextCursor
        } while (cursor != null)

        // id 커서만 썼다면 중간 시각·최신 id 행이 누락됨 → 복합 커서로 3건 모두 수집
        assertThat(collected).hasSize(3)
        assertThat(collected.toSet()).hasSize(3)
    }

    @Test
    fun `stats normalizes period and reflects week range`() {
        val user = persistUser()
        evaluationService.submit(user.id, request()) // 오늘 측정 1건

        val week = evaluationService.stats(user.id, "WEEK", null, null)
        assertThat(week.period).isEqualTo("week") // 정규화(소문자)
        assertThat(week.from).isEqualTo(LocalDate.now().minusDays(6)) // 최근 7일
        assertThat(week.measuredDays).isEqualTo(1)

        // 알 수 없는 period → month 폴백
        val fallback = evaluationService.stats(user.id, "bogus", null, null)
        assertThat(fallback.period).isEqualTo("month")
        assertThat(fallback.from).isEqualTo(LocalDate.now().withDayOfMonth(1))
    }

    @Test
    fun `stats week range excludes measurement older than 7 days`() {
        val user = persistUser()
        val twentyDaysAgo = Instant.now().minus(20, ChronoUnit.DAYS)
        evaluationService.submit(
            user.id,
            request(evaluatedAt = twentyDaysAgo, healthKitData = healthKit(LocalDate.now().minusDays(20))),
        )
        evaluationService.submit(user.id, request()) // 오늘

        val week = evaluationService.stats(user.id, "week", null, null)
        assertThat(week.measuredDays).isEqualTo(1) // 20일 전 측정은 최근 7일 범위 밖
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
