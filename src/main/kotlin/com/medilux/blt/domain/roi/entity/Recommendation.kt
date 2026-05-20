package com.medilux.blt.domain.roi.entity

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
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.SqlTypes

@Entity
@Table(
    name = "recommendations",
    indexes = [
        Index(name = "idx_recommendations_roi_score_id", columnList = "roi_score_id"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class Recommendation(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "roi_score_id", nullable = false, updatable = false)
    val roiScore: BrainRoiScore,

    @Column(name = "quadrant_key", nullable = false)
    val quadrantKey: String,

    @Column(name = "title", nullable = false, length = 100)
    var title: String,

    @Column(name = "message", nullable = false, columnDefinition = "text")
    var message: String,

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "suggested_tasks", nullable = false, columnDefinition = "jsonb")
    var suggestedTasks: List<Map<String, Any?>> = emptyList(),
) : BaseEntity() {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}
