package com.medilux.blt.domain.auth.controller

import com.medilux.blt.domain.auth.dto.AuthSessionResponse
import com.medilux.blt.domain.auth.dto.RefreshTokenRequest
import com.medilux.blt.domain.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
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
    fun refresh(@RequestBody request: RefreshTokenRequest): AuthSessionResponse = authService.refreshSession(request)
}
