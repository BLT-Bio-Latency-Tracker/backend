package com.medilux.blt.domain.user.dto

import com.medilux.blt.domain.user.entity.AuthType
import com.medilux.blt.domain.user.entity.Gender
import com.medilux.blt.domain.user.entity.Occupation
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserStatus
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.constraints.Size

@Schema(description = "사용자(마이페이지) 정보 응답")
data class UserResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "닉네임", nullable = true)
    val nickname: String?,
    @field:Schema(description = "이메일(Apple)", nullable = true)
    val email: String?,
    @field:Schema(description = "인증 수단", example = "APPLE")
    val authType: AuthType,
    @field:Schema(description = "계정 상태", example = "ACTIVE")
    val status: UserStatus,
    @field:Schema(description = "출생연도", example = "1995", nullable = true)
    val birthYear: Short?,
    @field:Schema(description = "성별", example = "FEMALE", nullable = true)
    val gender: Gender?,
    @field:Schema(description = "직업군", example = "KNOWLEDGE_WORKER", nullable = true)
    val occupation: Occupation?,
    @field:Schema(description = "온보딩 완료 여부", example = "true")
    val onboardingCompleted: Boolean,
) {
    companion object {
        fun from(user: User): UserResponse = UserResponse(
            userId = user.id,
            nickname = user.nickname,
            email = user.email,
            authType = user.authType,
            status = user.status,
            birthYear = user.birthYear,
            gender = user.gender,
            occupation = user.occupation,
            onboardingCompleted = user.onboardingCompleted,
        )
    }
}

@Schema(description = "회원 정보 수정 요청 (보낸 필드만 부분 수정)")
data class ProfileUpdateRequest(
    @field:Schema(description = "닉네임", nullable = true)
    @field:Size(max = 50)
    val nickname: String? = null,
    @field:Schema(description = "출생연도. 1900 ~ 현재 연도", example = "1995", nullable = true)
    val birthYear: Short? = null,
    @field:Schema(description = "성별", example = "FEMALE", nullable = true)
    val gender: Gender? = null,
    @field:Schema(description = "직업군", example = "KNOWLEDGE_WORKER", nullable = true)
    val occupation: Occupation? = null,
)
