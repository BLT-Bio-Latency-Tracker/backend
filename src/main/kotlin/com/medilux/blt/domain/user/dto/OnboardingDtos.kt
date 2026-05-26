package com.medilux.blt.domain.user.dto

import com.medilux.blt.domain.user.entity.Gender
import com.medilux.blt.domain.user.entity.Occupation
import com.medilux.blt.domain.user.entity.User

data class OnboardingRequest(val birthYear: Short?, val gender: Gender?, val occupation: Occupation?)

data class OnboardingResponse(
    val userId: Long,
    val birthYear: Short?,
    val gender: Gender?,
    val occupation: Occupation?,
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
