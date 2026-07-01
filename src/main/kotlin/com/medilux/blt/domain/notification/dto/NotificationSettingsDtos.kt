package com.medilux.blt.domain.notification.dto

import com.medilux.blt.domain.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size
import java.time.LocalTime

private const val CUSTOM_OPTIONS_MAX = 50

@Schema(description = "알림 설정 응답")
data class NotificationSettingsResponse(
    @field:Schema(description = "마스터 토글 — 측정/취침 외 모든 알림 ON/OFF", example = "true")
    val notificationEnabled: Boolean,
    @field:Schema(description = "측정(PVT) 알림 시각", example = "07:30:00")
    val pvtReminderTime: LocalTime,
    @field:Schema(description = "취침 알림 시각", example = "22:30:00")
    val sleepReminderTime: LocalTime,
    @field:Schema(description = "알림 기준 타임존", example = "Asia/Seoul")
    val notificationTimezone: String,
    @field:Schema(description = "확장 알림 옵션")
    val customNotificationOptions: Map<String, Any>,
) {
    companion object {
        fun from(user: User): NotificationSettingsResponse = NotificationSettingsResponse(
            notificationEnabled = user.notificationEnabled,
            pvtReminderTime = user.pvtReminderTime,
            sleepReminderTime = user.sleepReminderTime,
            notificationTimezone = user.notificationTimezone,
            customNotificationOptions = user.customNotificationOptions,
        )
    }
}

@Schema(description = "알림 설정 변경 요청 (보낸 필드만 부분 수정)")
data class NotificationSettingsUpdateRequest(
    val notificationEnabled: Boolean? = null,
    val pvtReminderTime: LocalTime? = null,
    val sleepReminderTime: LocalTime? = null,
    val notificationTimezone: String? = null,
    @field:Size(max = CUSTOM_OPTIONS_MAX)
    val customNotificationOptions: Map<String, Any>? = null,
)
