package com.medilux.blt.domain.auth.controller

import com.medilux.blt.domain.auth.dto.AuthSessionResponse
import com.medilux.blt.domain.auth.dto.LogoutRequest
import com.medilux.blt.domain.auth.dto.RefreshTokenRequest
import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth")
@Tag(name = "Auth", description = "인증 토큰 관리 API")
class AuthController(private val authService: AuthService) {
    @Operation(summary = "Refresh token으로 세션 토큰 갱신")
    @PostMapping("/refresh")
    fun refresh(@Valid @RequestBody request: RefreshTokenRequest): AuthSessionResponse = authService.refreshSession(request)

    @Operation(summary = "로그아웃")
    @PostMapping("/logout")
    fun logout(
        @AuthenticationPrincipal
        principal: AuthUserPrincipal?,
        @Valid @RequestBody
        request: LogoutRequest,
    ): ResponseEntity<Void> {
        authService.logout(principal, request)
        return ResponseEntity.noContent().build()
    }
}
