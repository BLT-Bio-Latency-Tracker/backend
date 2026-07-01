package com.medilux.blt.global.validation

import com.medilux.blt.domain.auth.dto.AppleVerifyRequest
import com.medilux.blt.domain.notification.dto.DeviceRegisterRequest
import com.medilux.blt.domain.notification.dto.NotificationSettingsUpdateRequest
import com.medilux.blt.domain.roi.dto.EvaluationCreateRequest
import com.medilux.blt.domain.roi.dto.PvtRequest
import jakarta.validation.Validation
import jakarta.validation.Validator
import org.junit.jupiter.api.AfterAll
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeAll
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

/**
 * 요청 DTO의 입력 검증(@Size/@NotBlank/@Valid) 가드레일 회귀 테스트.
 * 무제한 컬렉션/문자열이 남용 시 400으로 거부되는지, 중첩(@Valid) 전파가 동작하는지 확인한다.
 */
class RequestDtoValidationTest {
    private fun violates(target: Any, property: String): Boolean = validator.validate(target).any { it.propertyPath.toString() == property }

    private fun validPvt(rawRtMs: List<Int> = listOf(250, 300)) = PvtRequest(
        measurementId = UUID.randomUUID(),
        startedAt = Instant.parse("2026-05-30T09:40:00Z"),
        endedAt = Instant.parse("2026-05-30T09:43:00Z"),
        totalDurationMs = 180_000,
        totalCount = rawRtMs.size,
        rawRtMs = rawRtMs,
        avgRtMs = 275.0,
        lapsesMild = 0,
        lapsesTimeout = 0,
        falseStarts = 0,
        isValid = true,
    )

    @Test
    fun `identityToken must not be blank`() {
        assertTrue(violates(AppleVerifyRequest(identityToken = "   "), "identityToken"))
    }

    @Test
    fun `identityToken over max length is rejected`() {
        assertTrue(violates(AppleVerifyRequest(identityToken = "a".repeat(4097)), "identityToken"))
    }

    @Test
    fun `pvt rawRtMs over max size is rejected`() {
        assertTrue(violates(validPvt(rawRtMs = List(2001) { 250 }), "rawRtMs"))
    }

    @Test
    fun `evaluation request cascades validation into nested pvt`() {
        val request = EvaluationCreateRequest(
            evaluatedAt = Instant.parse("2026-05-30T09:43:00Z"),
            timezone = "Asia/Seoul",
            healthKitData = null,
            pvt = validPvt(rawRtMs = List(2001) { 250 }),
        )
        assertTrue(violates(request, "pvt.rawRtMs"))
    }

    @Test
    fun `evaluation request rejects blank timezone`() {
        val request = EvaluationCreateRequest(
            evaluatedAt = Instant.parse("2026-05-30T09:43:00Z"),
            timezone = " ",
            healthKitData = null,
            pvt = validPvt(),
        )
        assertTrue(violates(request, "timezone"))
    }

    @Test
    fun `fcmToken over max length is rejected`() {
        assertTrue(violates(DeviceRegisterRequest(fcmToken = "a".repeat(4097)), "fcmToken"))
    }

    @Test
    fun `customNotificationOptions over max entries is rejected`() {
        val tooMany = (1..51).associate { "k$it" to (it as Any) }
        assertTrue(violates(NotificationSettingsUpdateRequest(customNotificationOptions = tooMany), "customNotificationOptions"))
    }

    private companion object {
        private lateinit var factory: jakarta.validation.ValidatorFactory
        lateinit var validator: Validator

        @BeforeAll
        @JvmStatic
        fun setUp() {
            factory = Validation.buildDefaultValidatorFactory()
            validator = factory.validator
        }

        @AfterAll
        @JvmStatic
        fun tearDown() {
            factory.close()
        }
    }
}
