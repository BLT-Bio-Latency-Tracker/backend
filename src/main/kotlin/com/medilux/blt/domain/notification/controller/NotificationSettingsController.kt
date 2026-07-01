package com.medilux.blt.domain.notification.controller

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.notification.dto.NotificationSettingsResponse
import com.medilux.blt.domain.notification.dto.NotificationSettingsUpdateRequest
import com.medilux.blt.domain.notification.service.NotificationSettingsService
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users/me/notification-settings")
@Tag(name = "Notification Settings", description = "알림 설정 조회/변경 API")
class NotificationSettingsController(private val notificationSettingsService: NotificationSettingsService) {
    @Operation(summary = "알림 설정 조회")
    @GetMapping
    fun get(@AuthenticationPrincipal principal: AuthUserPrincipal?): NotificationSettingsResponse =
        notificationSettingsService.get(requireUserId(principal))

    @Operation(summary = "알림 설정 변경 (부분 수정)")
    @PatchMapping
    fun update(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @Valid @RequestBody request: NotificationSettingsUpdateRequest,
    ): NotificationSettingsResponse = notificationSettingsService.update(requireUserId(principal), request)

    private fun requireUserId(principal: AuthUserPrincipal?): Long =
        principal?.userId ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
}
