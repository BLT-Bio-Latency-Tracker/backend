package com.medilux.blt.domain.user.entity

import com.medilux.blt.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Index
import jakarta.persistence.Table
import org.hibernate.annotations.JdbcTypeCode
import org.hibernate.annotations.SQLRestriction
import org.hibernate.type.SqlTypes
import java.time.Instant
import java.time.LocalTime

@Entity
@Table(
    name = "users",
    indexes = [
        Index(name = "idx_users_apple_sub_hash", columnList = "apple_sub_hash", unique = true),
        Index(name = "idx_users_status", columnList = "status"),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class User(
    @Column(name = "apple_sub_hash", nullable = false, length = 64)
    var appleSubHash: String,
    @Column(name = "email", length = 255)
    var email: String? = null,
    @Column(name = "nickname", length = 50)
    var nickname: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,
    @Column(name = "withdrawn_at")
    var withdrawnAt: Instant? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20, updatable = false)
    val authType: AuthType,

    /** User Profile **/
    @Column(name = "birth_year")
    var birthYear: Short? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    var gender: Gender? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "occupation", length = 30)
    var occupation: Occupation? = null,
    @Column(name = "onboarding_completed", nullable = false)
    var onboardingCompleted: Boolean = false,

    /** User Notification Settings **/
    @Column(name = "apns_device_token", length = 64)
    var apnsDeviceToken: String? = null,
    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = true,
    @Column(name = "sleep_reminder_time", nullable = false)
    var sleepReminderTime: LocalTime = LocalTime.of(22, 30),
    @Column(name = "pvt_reminder_time", nullable = false)
    var pvtReminderTime: LocalTime = LocalTime.of(7, 30),
    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "custom_notification_options", nullable = false, columnDefinition = "jsonb")
    var customNotificationOptions: Map<String, Any> = emptyMap(),
    @Column(name = "notification_timezone", nullable = false, length = 40)
    var notificationTimezone: String = "Asia/Seoul",
) : BaseEntity() {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    val id: Long = 0L
}

enum class UserStatus {
    ACTIVE,
    WITHDRAW_PENDING,
    WITHDRAWN,
}

enum class AuthType {
    APPLE,
    GUEST,
}

enum class Gender {
    MALE,
    FEMALE,
}

enum class Occupation {
    KNOWLEDGE_WORKER,
    FIELD_WORKER,
    STUDENT,
    OTHER,
}
