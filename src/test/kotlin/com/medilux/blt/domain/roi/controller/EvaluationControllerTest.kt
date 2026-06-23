package com.medilux.blt.domain.roi.controller

import com.fasterxml.jackson.databind.ObjectMapper
import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.auth.security.JwtAuthenticationFilter
import com.medilux.blt.domain.roi.dto.EvaluationCreateRequest
import com.medilux.blt.domain.roi.dto.EvaluationDetailResponse
import com.medilux.blt.domain.roi.dto.EvaluationPageResponse
import com.medilux.blt.domain.roi.dto.EvaluationResponse
import com.medilux.blt.domain.roi.dto.EvaluationStatsResponse
import com.medilux.blt.domain.roi.dto.EvaluationSummaryResponse
import com.medilux.blt.domain.roi.dto.HealthKitDataRequest
import com.medilux.blt.domain.roi.dto.PvtDetail
import com.medilux.blt.domain.roi.dto.PvtRequest
import com.medilux.blt.domain.roi.dto.RecommendationResponse
import com.medilux.blt.domain.roi.dto.SleepDetail
import com.medilux.blt.domain.roi.entity.CalculationScenario
import com.medilux.blt.domain.roi.entity.RoiQuadrant
import com.medilux.blt.domain.roi.service.EvaluationService
import com.medilux.blt.domain.sleep.entity.SleepDataCompleteness
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import com.ninjasquad.springmockk.MockkBean
import io.mockk.every
import io.mockk.verify
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.context.TestConfiguration
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.ComponentScan
import org.springframework.context.annotation.FilterType
import org.springframework.context.annotation.Import
import org.springframework.data.jpa.mapping.JpaMetamodelMappingContext
import org.springframework.http.MediaType
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication
import org.springframework.security.web.SecurityFilterChain
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.get
import org.springframework.test.web.servlet.post
import java.time.Instant
import java.time.LocalDate
import java.util.UUID

@WebMvcTest(
    controllers = [EvaluationController::class],
    // 컨트롤러 슬라이스에 불필요한 JWT 필터(JwtTokenProvider 의존) 자동 등록 제외
    excludeFilters = [
        ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE, classes = [JwtAuthenticationFilter::class]),
    ],
)
@Import(EvaluationControllerTest.TestSecurityConfig::class)
class EvaluationControllerTest {
    @Autowired
    lateinit var mockMvc: MockMvc

    @Autowired
    lateinit var objectMapper: ObjectMapper

    @MockkBean
    lateinit var evaluationService: EvaluationService

    // @EnableJpaAuditing(BltApplication)이 웹 슬라이스에서 요구하는 JPA 메타모델 대체
    @MockkBean
    lateinit var jpaMetamodelMappingContext: JpaMetamodelMappingContext

    private val userId = 1L

    private fun auth(id: Long = userId) = authentication(UsernamePasswordAuthenticationToken(AuthUserPrincipal(id), null, emptyList()))

    @Test
    fun `submit returns 201 with score and recommendations`() {
        every { evaluationService.submit(any(), any()) } returns sampleResponse()

        mockMvc.post("/api/v1/evaluations") {
            with(auth())
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(sampleRequest())
        }.andExpect {
            status { isCreated() }
            jsonPath("$.evaluationId") { value(9001) }
            jsonPath("$.quadrant") { value("HIGH_SLEEP_HIGH_PVT") }
            jsonPath("$.calculationScenario") { value("A") }
            jsonPath("$.statusLabel") { value("최적 집중") }
            jsonPath("$.breakdown.finalFormula") { value("WEIGHTED") }
            jsonPath("$.recommendations[0].quadrantKey") { value("HIGH_SLEEP_HIGH_PVT") }
        }
    }

    @Test
    fun `submit without authentication returns 401`() {
        mockMvc.post("/api/v1/evaluations") {
            contentType = MediaType.APPLICATION_JSON
            content = objectMapper.writeValueAsString(sampleRequest())
        }.andExpect {
            status { isUnauthorized() }
            jsonPath("$.errorCode") { value("AUTH_INVALID_CREDENTIALS") }
        }
    }

    @Test
    fun `getToday returns 200 when measurement exists`() {
        every { evaluationService.getToday(userId) } returns sampleResponse()

        mockMvc.get("/api/v1/evaluations/today") { with(auth()) }
            .andExpect {
                status { isOk() }
                jsonPath("$.evaluationId") { value(9001) }
            }
    }

    @Test
    fun `getToday returns 204 when no measurement today`() {
        every { evaluationService.getToday(userId) } returns null

        mockMvc.get("/api/v1/evaluations/today") { with(auth()) }
            .andExpect { status { isNoContent() } }
    }

    @Test
    fun `getDetail returns 200 with sleep and pvt cards`() {
        every { evaluationService.getDetail(userId, 9001L) } returns sampleDetail()

        mockMvc.get("/api/v1/evaluations/9001") { with(auth()) }
            .andExpect {
                status { isOk() }
                jsonPath("$.evaluation.evaluationId") { value(9001) }
                jsonPath("$.sleep.sleepDate") { value("2026-05-30") }
                jsonPath("$.pvt.bestRtMs") { value(250) }
            }
    }

    @Test
    fun `getDetail returns 404 when not found`() {
        every { evaluationService.getDetail(userId, 999L) } throws BltException(ErrorCode.BRAIN_ROI_NOT_FOUND)

        mockMvc.get("/api/v1/evaluations/999") { with(auth()) }
            .andExpect {
                status { isNotFound() }
                jsonPath("$.errorCode") { value("BRAIN_ROI_NOT_FOUND") }
            }
    }

    @Test
    fun `list returns 200 with cursor page`() {
        val page = EvaluationPageResponse(items = listOf(sampleSummary()), nextCursor = "abc", hasNext = true)
        every { evaluationService.list(userId, null, null, null, 10) } returns page

        mockMvc.get("/api/v1/evaluations?size=10") { with(auth()) }
            .andExpect {
                status { isOk() }
                jsonPath("$.items[0].evaluationId") { value(9001) }
                jsonPath("$.nextCursor") { value("abc") }
                jsonPath("$.hasNext") { value(true) }
            }
        verify { evaluationService.list(userId, null, null, null, 10) }
    }

    @Test
    fun `list uses default size 31 when not provided`() {
        every { evaluationService.list(userId, null, null, null, 31) } returns EvaluationPageResponse(emptyList(), null, false)

        mockMvc.get("/api/v1/evaluations") { with(auth()) }
            .andExpect { status { isOk() } }
        verify { evaluationService.list(userId, null, null, null, 31) }
    }

    @Test
    fun `stats returns 200 and defaults period to month`() {
        every { evaluationService.stats(userId, "month", null, null) } returns
            EvaluationStatsResponse("month", LocalDate.parse("2026-05-01"), LocalDate.parse("2026-05-31"), 18, 72, 88, 55)

        mockMvc.get("/api/v1/evaluations/stats") { with(auth()) }
            .andExpect {
                status { isOk() }
                jsonPath("$.period") { value("month") }
                jsonPath("$.measuredDays") { value(18) }
                jsonPath("$.avgRoi") { value(72) }
            }
        verify { evaluationService.stats(userId, "month", null, null) }
    }

    // --- fixtures -----------------------------------------------------------

    private fun sampleResponse(id: Long = 9001L) = EvaluationResponse(
        evaluationId = id,
        finalScore = 95,
        sleepScore = 96,
        pvtScore = 94,
        quadrant = RoiQuadrant.HIGH_SLEEP_HIGH_PVT,
        calculationScenario = CalculationScenario.A,
        statusLabel = "최적 집중",
        formulaVersion = "v2",
        trendVsYesterday = 12,
        measuredAt = Instant.parse("2026-05-30T09:43:00Z"),
        breakdown = mapOf("finalFormula" to "WEIGHTED", "sleepMissing" to false),
        recommendations = listOf(
            RecommendationResponse(
                recommendationId = 5501L,
                quadrantKey = "HIGH_SLEEP_HIGH_PVT",
                title = "지금이 골든타임이에요",
                message = "가장 중요한 집중 업무를 지금 처리하세요.",
                suggestedTasks = listOf(mapOf("time" to "now", "task" to "핵심 업무", "type" to "focus")),
            ),
        ),
    )

    private fun sampleDetail() = EvaluationDetailResponse(
        evaluation = sampleResponse(),
        sleep = SleepDetail(
            sleepDate = LocalDate.parse("2026-05-30"),
            totalMinutes = 400,
            deepMinutes = 72,
            remMinutes = 88,
            coreMinutes = 220,
            awakeMinutes = 20,
            inBedMinutes = 460,
            efficiencyPercent = 87,
            deepRatioPercent = 18,
            remRatioPercent = 22,
            lightRatioPercent = 55,
            dataCompleteness = SleepDataCompleteness.FULL,
        ),
        pvt = PvtDetail(
            measurementId = UUID.fromString("b3f1c2d4-0000-4a1b-9c2d-000000000001"),
            avgRtMs = 312.0,
            bestRtMs = 250,
            medianRtMs = 310.0,
            lapsesMild = 1,
            lapsesTimeout = 0,
            falseStarts = 1,
            totalCount = 7,
            rawRtMs = listOf(250, 310, 520, 400, 290, 360, 305),
            isValid = true,
        ),
    )

    private fun sampleSummary() = EvaluationSummaryResponse(
        evaluationId = 9001L,
        date = LocalDate.parse("2026-05-30"),
        measuredAt = Instant.parse("2026-05-30T09:43:00Z"),
        finalScore = 72,
        statusLabel = "안정적",
        quadrant = RoiQuadrant.HIGH_SLEEP_LOW_PVT,
    )

    private fun sampleRequest() = EvaluationCreateRequest(
        evaluatedAt = Instant.parse("2026-05-30T09:43:00Z"),
        timezone = "Asia/Seoul",
        healthKitData = HealthKitDataRequest(
            sleepDate = LocalDate.parse("2026-05-30"),
            totalMinutes = 400,
            deepMinutes = 72,
            remMinutes = 88,
            nightHrvMs = 45.2,
            weeklyHrvBaselineMs = 48.1,
        ),
        pvt = PvtRequest(
            measurementId = UUID.fromString("b3f1c2d4-0000-4a1b-9c2d-000000000001"),
            startedAt = Instant.parse("2026-05-30T09:42:30Z"),
            endedAt = Instant.parse("2026-05-30T09:43:00Z"),
            totalDurationMs = 30500,
            totalCount = 7,
            rawRtMs = listOf(250, 310, 520, 400, 290, 360, 305),
            avgRtMs = 312.0,
            medianRtMs = 310.0,
            lapsesMild = 1,
            lapsesTimeout = 0,
            falseStarts = 1,
            isValid = true,
        ),
    )

    @TestConfiguration
    class TestSecurityConfig {
        @Bean
        fun testFilterChain(http: HttpSecurity): SecurityFilterChain = http
            .csrf { it.disable() }
            .authorizeHttpRequests { it.anyRequest().permitAll() }
            .build()
    }
}
