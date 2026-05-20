package com.medilux.blt.domain.user.entity

import com.medilux.blt.global.common.entity.BaseEntity
import jakarta.persistence.Column
import jakarta.persistence.Entity
import jakarta.persistence.EnumType
import jakarta.persistence.Enumerated
import jakarta.persistence.GeneratedValue
import jakarta.persistence.GenerationType
import jakarta.persistence.Id
import jakarta.persistence.Table
import jakarta.persistence.UniqueConstraint
import org.hibernate.annotations.SQLRestriction
import java.time.LocalTime

@Entity
@Table(
    name = "users",
    uniqueConstraints = [
        UniqueConstraint(name = "uk_users_apple_sub_hash", columnNames = ["apple_sub_hash"]),
    ],
)
@SQLRestriction("deleted_at IS NULL")
class User(
    @Column(name = "apple_sub_hash", nullable = false, length = 64, updatable = false)
    val appleSubHash: String,
    @Column(name = "email", length = 255)
    var email: String? = null,
    @Column(name = "nickname", length = 50)
    var nickname: String? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    var status: UserStatus = UserStatus.ACTIVE,
    @Enumerated(EnumType.STRING)
    @Column(name = "auth_type", nullable = false, length = 20, updatable = false)
    val authType: AuthType,
    @Column(name = "birth_year")
    var birthYear: Short? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 10)
    var gender: Gender? = null,
    @Column(name = "avg_wake_time")
    var avgWakeTime: LocalTime? = null,
    @Enumerated(EnumType.STRING)
    @Column(name = "occupation", length = 30)
    var occupation: Occupation? = null,
    @Column(name = "onboarding_completed", nullable = false)
    var onboardingCompleted: Boolean = false,
    @Column(name = "apns_device_token", length = 64)
    var apnsDeviceToken: String? = null,
    @Column(name = "notification_enabled", nullable = false)
    var notificationEnabled: Boolean = true,
    @Column(name = "pvt_reminder_enabled", nullable = false)
    var pvtReminderEnabled: Boolean = true,
    @Column(name = "pvt_reminder_time", nullable = false)
    var pvtReminderTime: LocalTime,
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
    STUDENT,
    OFFICE_WORKER,
    HEALTHCARE_WORKER,
    SHIFT_WORKER,
    SELF_EMPLOYED,
    OTHER,
}
