package com.medilux.blt.domain.notification.dto

import com.medilux.blt.domain.user.entity.DevicePlatform
import com.medilux.blt.domain.user.entity.UserDevice
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Size
import java.time.Instant

// FCM 등록 토큰 길이 상한 — 남용 방어 가드레일(초과 시 400). 실제 토큰(~200자)보다 넉넉히.
private const val FCM_TOKEN_MAX = 4096

@Schema(description = "FCM 디바이스 등록 요청")
data class DeviceRegisterRequest(
    @field:Schema(description = "FCM 등록 토큰", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    @field:Size(max = FCM_TOKEN_MAX)
    val fcmToken: String,
    @field:Schema(description = "디바이스 플랫폼", example = "IOS")
    val platform: DevicePlatform = DevicePlatform.IOS,
)

@Schema(description = "디바이스 등록 응답")
data class DeviceResponse(
    @field:Schema(description = "등록된 디바이스 ID (해제 시 사용)", example = "88")
    val deviceId: Long,
    @field:Schema(description = "플랫폼", example = "IOS")
    val platform: DevicePlatform,
    @field:Schema(description = "마지막 활성 시각")
    val lastActiveAt: Instant,
) {
    companion object {
        fun from(device: UserDevice): DeviceResponse = DeviceResponse(
            deviceId = device.id,
            platform = device.platform,
            lastActiveAt = device.lastActiveAt,
        )
    }
}
