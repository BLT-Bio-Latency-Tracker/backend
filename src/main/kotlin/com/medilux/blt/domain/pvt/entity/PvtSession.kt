package com.medilux.blt.domain.pvt.entity

import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.FetchType
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.JoinColumn
import jakarta.persistence.ManyToOne
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.SqlTypes
import java.time.LocalDateTime
import java.util.UUID

@Entity
@Table(
    name = "pvt_sessions",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_pvt_sessions_measurement_id", columnNames = ["measurement_id"]),
    ],
    indexes = [
        Index(name = "idx_pvt_sessions_user_id", columnList = "user_id"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class PvtSession(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,

    @Column(name = "measurement_id", nullable = false, updatable = false)
    val measurementId: UUID,

    @Column(name = "started_at", nullable = false, updatable = false)
    val startedAt: LocalDateTime,

    @Column(name = "ended_at", nullable = false, updatable = false)
    val endedAt: LocalDateTime,

    @Column(name = "total_duration_ms", nullable = false)
    val totalDurationMs: Int,

    @Column(name = "total_count", nullable = false)
    val totalCount: Int,

    @JdbcTypeCode(SqlTypes.ARRAY)
    @Column(name = "raw_rt_ms", nullable = false, columnDefinition = "integer[]")
    val rawRtMs: List<Int>,

    @Column(name = "avg_rt_ms", nullable = false)
    val avgRtMs: Double,

    @Column(name = "median_rt_ms")
    val medianRtMs: Double? = null,

    @Column(name = "lapses_mild", nullable = false)
    val lapsesMild: Int,

    @Column(name = "lapses_timeout", nullable = false)
    val lapsesTimeout: Int,

    @Column(name = "false_starts", nullable = false)
    val falseStarts: Int,

    @Column(name = "is_valid", nullable = false)
    val isValid: Boolean,

    @Column(name = "invalid_reason", length = 50)
    val invalidReason: String? = null,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "trials", nullable = false, columnDefinition = "jsonb")
    val trials: List<Map<String, Any?>> = emptyList(),
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}
