package com.medilux.blt.domain.roi.repository

import com.medilux.blt.domain.roi.entity.BrainRoiScore
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface BrainRoiScoreRepository : JpaRepository<BrainRoiScore, Long> {
    /** 전일 대비 트렌드용 — 주어진 시각 직전 측정. */
    fun findFirstByUserIdAndMeasuredAtLessThanOrderByMeasuredAtDescIdDesc(userId: Long, measuredAt: Instant): BrainRoiScore?
}
