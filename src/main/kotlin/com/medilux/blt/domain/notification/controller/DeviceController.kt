package com.medilux.blt.domain.notification.controller

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.notification.dto.DeviceRegisterRequest
import com.medilux.blt.domain.notification.dto.DeviceResponse
import com.medilux.blt.domain.notification.service.DeviceService
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/devices")
@Tag(name = "Devices", description = "FCM 디바이스 등록/해제 API")
class DeviceController(private val deviceService: DeviceService) {
    @Operation(summary = "FCM 디바이스 등록")
    @PostMapping
    fun register(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @Valid @RequestBody request: DeviceRegisterRequest,
    ): DeviceResponse = deviceService.register(requireUserId(principal), request)

    @Operation(summary = "디바이스 등록 해제")
    @DeleteMapping("/{deviceId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    fun unregister(@AuthenticationPrincipal principal: AuthUserPrincipal?, @PathVariable deviceId: Long) =
        deviceService.unregister(requireUserId(principal), deviceId)

    private fun requireUserId(principal: AuthUserPrincipal?): Long =
        principal?.userId ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
}
