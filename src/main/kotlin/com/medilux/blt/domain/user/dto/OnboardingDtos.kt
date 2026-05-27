package com.medilux.blt.domain.user.dto

import com.medilux.blt.domain.user.entity.Gender
import com.medilux.blt.domain.user.entity.Occupation
import com.medilux.blt.domain.user.entity.User
import io.swagger.v3.oas.annotations.media.Schema

@Schema(description = "온보딩 프로필 완료 요청")
data class OnboardingRequest(
    @field:Schema(description = "출생연도. 1900년부터 현재 연도까지 허용합니다.", example = "1995", nullable = true)
    val birthYear: Short?,
    @field:Schema(description = "성별", example = "FEMALE", nullable = true)
    val gender: Gender?,
    @field:Schema(description = "직업", example = "OFFICE_WORKER", nullable = true)
    val occupation: Occupation?,
)

@Schema(description = "온보딩 프로필 완료 응답")
data class OnboardingResponse(
    @field:Schema(description = "사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "출생연도", example = "1995", nullable = true)
    val birthYear: Short?,
    @field:Schema(description = "성별", example = "FEMALE", nullable = true)
    val gender: Gender?,
    @field:Schema(description = "직업", example = "OFFICE_WORKER", nullable = true)
    val occupation: Occupation?,
    @field:Schema(description = "온보딩 완료 여부", example = "true")
    val onboardingCompleted: Boolean,
) {
    companion object {
        fun from(user: User): OnboardingResponse = OnboardingResponse(
            userId = user.id,
            birthYear = user.birthYear,
            gender = user.gender,
            occupation = user.occupation,
            onboardingCompleted = user.onboardingCompleted,
        )
    }
}
