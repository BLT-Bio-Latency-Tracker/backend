package com.medilux.blt.domain.roi.entity

import com.medilux.blt.domain.pvt.entity.PvtSession
import com.medilux.blt.domain.sleep.entity.SleepRecord
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.SqlTypes
import java.time.Instant

@Entity
@Table(
    name = "brain_roi_scores",
    indexes = [
        Index(name = "idx_brain_roi_scores_user_id", columnList = "user_id"),
        Index(name = "idx_brain_roi_scores_session_id", columnList = "session_id"),
        Index(name = "idx_brain_roi_scores_sleep_id", columnList = "sleep_id"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class BrainRoiScore(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id", nullable = false, updatable = false)
    val session: PvtSession,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "sleep_id", nullable = false, updatable = false)
    val sleepRecord: SleepRecord,
    @Enumerated(EnumType.STRING)
    @Column(name = "calculation_scenario", nullable = false)
    val calculationScenario: CalculationScenario,
    @Column(name = "final_score", nullable = false)
    val finalScore: Int,
    @Column(name = "sleep_score", nullable = false)
    val sleepScore: Int,
    @Column(name = "pvt_score", nullable = false)
    val pvtScore: Int,
    @Enumerated(EnumType.STRING)
    @Column(name = "quadrant", nullable = false)
    val quadrant: RoiQuadrant,
    @Column(name = "formula_version", nullable = false)
    val formulaVersion: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "breakdown", nullable = false, columnDefinition = "jsonb")
    val breakdown: Map<String, Any?> = emptyMap(),
    @Column(name = "measured_at", nullable = false, updatable = false)
    val measuredAt: Instant,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}

enum class CalculationScenario {
    A,
    B,
    C,
    D,
}

enum class RoiQuadrant {
    HIGH_SLEEP_HIGH_PVT,
    HIGH_SLEEP_LOW_PVT,
    LOW_SLEEP_HIGH_PVT,
    LOW_SLEEP_LOW_PVT,
}
