package com.medilux.blt.domain.roi.repository

import com.medilux.blt.domain.roi.entity.BrainRoiScore
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

/** 통계용 경량 프로젝션 (finalScore + measuredAt만). */
interface RoiScoreRow {
    val finalScore: Int
    val measuredAt: Instant
}

interface BrainRoiScoreRepository : JpaRepository<BrainRoiScore, Long> {
    /** 소유권 확인 겸 상세 조회. */
    fun findByIdAndUserId(id: Long, userId: Long): BrainRoiScore?

    /** 오늘 최신 1건 (measuredAt이 [from, to) 구간). */
    fun findFirstByUserIdAndMeasuredAtGreaterThanEqualAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(
        userId: Long,
        from: Instant,
        to: Instant,
    ): BrainRoiScore?

    /** 전일 대비 트렌드용 — 주어진 시각 직전 측정. */
    fun findFirstByUserIdAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(userId: Long, measuredAt: Instant): BrainRoiScore?

    /** History 첫 페이지(커서 없음) — measuredAt desc, id desc. size+1로 hasNext 판별. */
    @Query(
        """
        SELECT b FROM BrainRoiScore b
         WHERE b.user.id = :userId
           AND b.measuredAt >= :from
           AND b.measuredAt < :to
         ORDER BY b.measuredAt DESC, b.id DESC
        """,
    )
    fun findFirstPage(
        @Param("userId") userId: Long,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        pageable: Pageable,
    ): List<BrainRoiScore>

    /**
     * History 다음 페이지 — 정렬 키(measuredAt desc, id desc)와 동일한 (measuredAt, id) 복합 커서.
     */
    @Query(
        """
        SELECT b FROM BrainRoiScore b
         WHERE b.user.id = :userId
           AND b.measuredAt >= :from
           AND b.measuredAt < :to
           AND (
                b.measuredAt < :cursorMeasuredAt
                OR (b.measuredAt = :cursorMeasuredAt AND b.id < :cursorId)
           )
         ORDER BY b.measuredAt DESC, b.id DESC
        """,
    )
    fun findPageAfter(
        @Param("userId") userId: Long,
        @Param("from") from: Instant,
        @Param("to") to: Instant,
        @Param("cursorMeasuredAt") cursorMeasuredAt: Instant,
        @Param("cursorId") cursorId: Long,
        pageable: Pageable,
    ): List<BrainRoiScore>

    /** 통계 — 기간 내 점수/시각. 일수·평균·최고·최저는 서비스에서 집계. */
    @Query(
        """
        SELECT b.finalScore AS finalScore, b.measuredAt AS measuredAt
          FROM BrainRoiScore b
         WHERE b.user.id = :userId
           AND b.measuredAt >= :from
           AND b.measuredAt < :to
        """,
    )
    fun findScoresInRange(@Param("userId") userId: Long, @Param("from") from: Instant, @Param("to") to: Instant): List<RoiScoreRow>
}
