package com.medilux.blt.domain.user.entity

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
    name = "notification_logs",
    indexes = [
        Index(name = "idx_notification_logs_user_id", columnList = "user_id"),
        Index(name = "idx_notification_logs_user_device_id", columnList = "user_device_id"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class NotificationLog(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_device_id")
    val userDevice: UserDevice? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false)
    val notificationType: NotificationType,
    @Column(name = "title", nullable = false)
    val title: String,
    @Column(name = "body", nullable = false, columnDefinition = "text")
    val body: String,
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "payload", nullable = false, columnDefinition = "jsonb")
    val payload: Map<String, Any?> = emptyMap(),
    @Column(name = "fcm_message_id")
    var fcmMessageId: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    var status: NotificationStatus,
    @Column(name = "error_code")
    var errorCode: String? = null,
    @Column(name = "error_message", columnDefinition = "text")
    var errorMessage: String? = null,
    @Column(name = "scheduled_at", nullable = false)
    val scheduledAt: Instant,
    @Column(name = "sent_at")
    var sentAt: Instant? = null,
    @Column(name = "deliverd_at")
    var deliveredAt: Instant? = null,
    @Column(name = "opened_at")
    var openedAt: Instant? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "log_id")
    val id: Long = 0L
}

enum class NotificationType {
    PVT_REMINDER,
    ROI_SCORE,
    RECOMMENDATION,
    SYSTEM,
}

enum class NotificationStatus {
    SCHEDULED,
    SENT,
    DELIVERED,
    OPENED,
    FAILED,
}
