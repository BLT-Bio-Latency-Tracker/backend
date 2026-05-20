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
import org.hibernate.annotations.SQLRestriction
import java.time.Instant

@Entity
@Table(
    name = "user_devices",
    indexes = [
        Index(name = "idx_user_devices_user_id", columnList = "user_id"),
        Index(name = "idx_user_devices_fcm_token", columnList = "fcm_token"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class UserDevice(
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, updatable = false)
    val user: User,
    @Column(name = "fcm_token", nullable = false, length = 255)
    var fcmToken: String,
    @Enumerated(EnumType.STRING)
    @Column(name = "platform", nullable = false, length = 10)
    val platform: DevicePlatform,
    @Column(name = "last_active_at", nullable = false)
    var lastActiveAt: Instant,
    @Column(name = "revoked_at")
    var revokedAt: Instant? = null,
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}

enum class DevicePlatform {
    IOS,
    ANDROID,
}
