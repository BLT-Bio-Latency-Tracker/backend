package com.medilux.blt.domain.notification.controller

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.notification.dto.NotificationCategory
import com.medilux.blt.domain.notification.dto.NotificationPageResponse
import com.medilux.blt.domain.notification.service.NotificationService
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/notifications")
@Tag(name = "Notifications", description = "알림함 조회/읽음 API")
class NotificationController(private val notificationService: NotificationService) {
    @Operation(summary = "알림함 조회 및 필터")
    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @RequestParam(defaultValue = "ALL") category: NotificationCategory,
        @RequestParam(defaultValue = "0") page: Int,
        @RequestParam(defaultValue = "20") size: Int,
    ): NotificationPageResponse = notificationService.list(requireUserId(principal), category, page, size)

    @Operation(summary = "개별 알림 읽음 처리")
    @PostMapping("/{logId}/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markRead(@AuthenticationPrincipal principal: AuthUserPrincipal?, @PathVariable logId: Long) =
        notificationService.markRead(requireUserId(principal), logId)

    @Operation(summary = "전체 알림 읽음 처리")
    @PostMapping("/read")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun markAllRead(@AuthenticationPrincipal principal: AuthUserPrincipal?) = notificationService.markAllRead(requireUserId(principal))

    @Operation(summary = "전체 알림 삭제 (모두 지우기)")
    @DeleteMapping
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun deleteAll(@AuthenticationPrincipal principal: AuthUserPrincipal?) = notificationService.deleteAll(requireUserId(principal))

    private fun requireUserId(principal: AuthUserPrincipal?): Long =
        principal?.userId ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
}
