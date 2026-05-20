package com.medilux.blt.domain.sleep.entity

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
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.SqlTypes
import java.time.LocalDate

@Entity
@Table(
    name = "sleep_records",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_sleep_records_user_sleep_date", columnNames = ["user_id", "sleep_date"]),
    ],
    indexes = [
        Index(name = "idx_sleep_records_user_id", columnList = "user_id"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class SleepRecord(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @Column(name = "sleep_date", nullable = false, updatable = false)
    val sleepDate: LocalDate,
    @Column(name = "total_minutes", nullable = false)
    val totalMinutes: Int,
    @Column(name = "deep_minutes")
    val deepMinutes: Int? = null,
    @Column(name = "rem_minutes")
    val remMinutes: Int? = null,
    @Column(name = "core_minutes")
    val coreMinutes: Int? = null,
    @Column(name = "awake_minutes")
    val awakeMinutes: Int? = null,
    @Column(name = "in_bed_minutes")
    val inBedMinutes: Int? = null,
    @Column(name = "unspecified_minutes")
    val unspecifiedMinutes: Int? = null,
    @Column(name = "sample_count")
    val sampleCount: Int? = null,
    @Column(name = "night_hrv_ms")
    val nightHrvMs: Double? = null,
    @Column(name = "weekly_hrv_baseline_ms")
    val weeklyHrvBaselineMs: Double? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "data_completeness", length = 20)
    val dataCompleteness: SleepDataCompleteness? = null,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    val rawPayload: Map<String, Any?>? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}

enum class SleepDataCompleteness {
    FULL,
    PARTIAL_STAGES,
    TOTAL_ONLY,
}
