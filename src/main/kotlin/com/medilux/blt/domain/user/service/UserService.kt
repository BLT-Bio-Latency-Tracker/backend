package com.medilux.blt.domain.user.service

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.user.dto.OnboardingRequest
import com.medilux.blt.domain.user.dto.OnboardingResponse
import com.medilux.blt.domain.user.repository.UserRepository
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Year

@Service
class UserService(private val userRepository: UserRepository) {
    @Transactional
    fun completeOnboarding(principal: AuthUserPrincipal?, request: OnboardingRequest): OnboardingResponse {
        val userId = principal?.userId ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        val user = userRepository.findById(userId).orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }

        if (user.onboardingCompleted) {
            throw BltException(ErrorCode.ONBOARDING_ALREADY_COMPLETED)
        }

        val inputCount = listOf(request.birthYear, request.gender, request.occupation)
            .count { value -> value != null }

        if (inputCount != 0 && inputCount != ONBOARDING_FIELD_COUNT) {
            throw BltException(ErrorCode.PARTIAL_ONBOARDING_NOT_ALLOWED)
        }

        if (inputCount == ONBOARDING_FIELD_COUNT) {
            val birthYear = request.birthYear ?: throw BltException(ErrorCode.PARTIAL_ONBOARDING_NOT_ALLOWED)
            if (birthYear.toInt() !in MIN_BIRTH_YEAR..Year.now().value) {
                throw BltException(ErrorCode.VALIDATION_FAILED)
            }

            user.birthYear = birthYear
            user.gender = request.gender
            user.occupation = request.occupation
        }

        user.onboardingCompleted = true

        return OnboardingResponse.from(user)
    }

    private companion object {
        const val MIN_BIRTH_YEAR = 1900
        const val ONBOARDING_FIELD_COUNT = 3
    }
}
