package com.medilux.blt.domain.sleep

import com.fasterxml.jackson.core.type.TypeReference
import com.medilux.blt.domain.sleep.entity.SleepStage
import com.medilux.blt.domain.sleep.entity.SleepStageSegment
import io.hypersistence.utils.hibernate.type.util.ObjectMapperWrapper
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.Instant

/**
 * 수면 구간(Instant + enum + Kotlin data class)이 실제 JSONB 저장에 쓰이는 hypersistence ObjectMapper로
 * 왕복 직렬화되는지 검증. (JavaTimeModule/KotlinModule 자동등록 여부를 Docker 없이 로컬에서 확인)
 */
class SleepStageJsonRoundTripTest {
    private val mapper = ObjectMapperWrapper.INSTANCE.objectMapper

    @Test
    fun `sleep stage segments round-trip through the jsonb object mapper`() {
        val segments = listOf(
            SleepStageSegment(SleepStage.REM, Instant.parse("2026-05-30T02:13:00Z"), Instant.parse("2026-05-30T02:41:00Z")),
            SleepStageSegment(SleepStage.DEEP, Instant.parse("2026-05-30T02:41:00Z"), Instant.parse("2026-05-30T03:10:00Z")),
        )

        val json = mapper.writeValueAsString(segments)
        val restored: List<SleepStageSegment> = mapper.readValue(json, object : TypeReference<List<SleepStageSegment>>() {})

        assertEquals(segments, restored)
    }
}
