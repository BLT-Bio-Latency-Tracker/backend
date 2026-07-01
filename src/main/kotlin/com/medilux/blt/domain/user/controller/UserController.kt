package com.medilux.blt.domain.user.controller

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.user.dto.OnboardingRequest
import com.medilux.blt.domain.user.dto.OnboardingResponse
import com.medilux.blt.domain.user.dto.ProfileUpdateRequest
import com.medilux.blt.domain.user.dto.TermsHistoryItemResponse
import com.medilux.blt.domain.user.dto.UserResponse
import com.medilux.blt.domain.user.dto.WithdrawResponse
import com.medilux.blt.domain.user.service.UserService
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PatchMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/users")
@Tag(name = "Users", description = "사용자 프로필 및 온보딩 API")
class UserController(private val userService: UserService) {
    @Operation(summary = "내 정보 조회 (마이페이지)")
    @GetMapping("/me")
    fun getMyInfo(@AuthenticationPrincipal principal: AuthUserPrincipal?): UserResponse = userService.getMyInfo(requireUserId(principal))

    @Operation(summary = "내 온보딩 프로필 완료")
    @PatchMapping("/me/onboarding")
    fun completeOnboarding(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @Valid @RequestBody request: OnboardingRequest,
    ): OnboardingResponse = userService.completeOnboarding(principal, request)

    @Operation(summary = "내 정보 수정")
    @PatchMapping("/me/profile")
    fun updateProfile(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @Valid @RequestBody request: ProfileUpdateRequest,
    ): UserResponse = userService.updateProfile(requireUserId(principal), request)

    @Operation(summary = "약관 동의 이력 조회")
    @GetMapping("/me/terms/history")
    fun getTermsHistory(@AuthenticationPrincipal principal: AuthUserPrincipal?): List<TermsHistoryItemResponse> =
        userService.getTermsHistory(requireUserId(principal))

    @Operation(summary = "내 계정 탈퇴 (유예기간 30일)")
    @DeleteMapping("/me")
    fun withdraw(@AuthenticationPrincipal principal: AuthUserPrincipal?): WithdrawResponse = userService.withdraw(
        requireUserId(principal),
    )

    private fun requireUserId(principal: AuthUserPrincipal?): Long =
        principal?.userId ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
}
