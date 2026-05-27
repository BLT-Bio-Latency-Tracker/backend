package com.medilux.blt.domain.auth.controller

import com.medilux.blt.domain.auth.dto.AppleAuthResponse
import com.medilux.blt.domain.auth.dto.AppleSignupRequest
import com.medilux.blt.domain.auth.dto.AppleVerifyRequest
import com.medilux.blt.domain.auth.dto.AuthSessionResponse
import com.medilux.blt.domain.auth.service.AuthService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.servlet.http.HttpServletRequest
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/auth/apple")
@Tag(name = "Apple Auth", description = "Apple Sign-In 인증 및 신규 가입 API")
class AppleAuthController(private val authService: AuthService) {
    @Operation(summary = "Apple identityToken 검증")
    @PostMapping("/verify")
    fun verify(@RequestBody request: AppleVerifyRequest): AppleAuthResponse = authService.verifyApple(request)

    @Operation(summary = "Apple 신규 사용자 약관 동의 후 가입")
    @PostMapping("/signup")
    fun signup(@RequestBody request: AppleSignupRequest, httpServletRequest: HttpServletRequest): AuthSessionResponse =
        authService.signup(request, httpServletRequest)
}
