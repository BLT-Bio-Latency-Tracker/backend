package com.medilux.blt.domain.user.controller

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.user.dto.OnboardingRequest
import com.medilux.blt.domain.user.dto.OnboardingResponse
import com.medilux.blt.domain.user.service.UserService
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "사용자 프로필 및 온보딩 API")
class UserController(private val userService: UserService) {
    @Operation(summary = "내 온보딩 프로필 완료")
    @PatchMapping("/me/onboarding")
    fun completeOnboarding(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @RequestBody request: OnboardingRequest,
    ): OnboardingResponse = userService.completeOnboarding(principal, request)
}
