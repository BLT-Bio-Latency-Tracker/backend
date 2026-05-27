package com.medilux.blt.domain.user.dto

import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "회원 탈퇴 요청 응답")
data class WithdrawResponse(
    @field:Schema(description = "사용자 상태", example = "WITHDRAW_PENDING")
    val status: String,
    @field:Schema(description = "회원 탈퇴 요청 시각")
    val withdrawnAt: Instant,
    @field:Schema(description = "최종 탈퇴 예정 시각")
    val withdrawScheduledAt: Instant,
)
