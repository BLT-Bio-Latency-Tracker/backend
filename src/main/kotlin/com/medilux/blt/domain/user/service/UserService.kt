package com.medilux.blt.domain.user.service

import com.medilux.blt.domain.auth.repository.RefreshTokenRepository
import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.user.dto.OnboardingRequest
import com.medilux.blt.domain.user.dto.OnboardingResponse
import com.medilux.blt.domain.user.dto.ProfileUpdateRequest
import com.medilux.blt.domain.user.dto.UserResponse
import com.medilux.blt.domain.user.dto.WithdrawResponse
import com.medilux.blt.domain.user.entity.UserStatus
import com.medilux.blt.domain.user.repository.UserDeviceRepository
import com.medilux.blt.domain.user.repository.UserRepository
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Duration
import java.time.Instant
import java.time.Year

@Service
class UserService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userDeviceRepository: UserDeviceRepository,
) {
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

    @Transactional(readOnly = true)
    fun getMyInfo(userId: Long): UserResponse {
        val user = userRepository.findById(userId).orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }
        return UserResponse.from(user)
    }

    @Transactional
    fun updateProfile(userId: Long, request: ProfileUpdateRequest): UserResponse {
        val user = userRepository.findById(userId).orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }

        request.nickname?.let { nickname -> user.nickname = nickname }
        request.birthYear?.let { birthYear ->
            if (birthYear.toInt() !in MIN_BIRTH_YEAR..Year.now().value) {
                throw BltException(ErrorCode.VALIDATION_FAILED)
            }
            user.birthYear = birthYear
        }
        request.gender?.let { gender -> user.gender = gender }
        request.occupation?.let { occupation -> user.occupation = occupation }

        return UserResponse.from(user)
    }

    @Transactional
    fun withdraw(userId: Long): WithdrawResponse {
        val user = userRepository.findById(userId).orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }

        if (user.status == UserStatus.WITHDRAW_PENDING) {
            val withdrawnAt = user.withdrawnAt ?: user.updatedAt ?: Instant.now()
            return WithdrawResponse(
                status = user.status.name,
                withdrawnAt = withdrawnAt,
                withdrawScheduledAt = withdrawnAt.plus(WITHDRAW_GRACE_PERIOD),
            )
        }

        val now = Instant.now()
        user.status = UserStatus.WITHDRAW_PENDING
        user.withdrawnAt = now

        refreshTokenRepository.revokeAllActiveByUserId(userId, now)
        userDeviceRepository.revokeAllActiveByUserId(userId, now)

        return WithdrawResponse(
            status = user.status.name,
            withdrawnAt = now,
            withdrawScheduledAt = now.plus(WITHDRAW_GRACE_PERIOD),
        )
    }

    private companion object {
        const val MIN_BIRTH_YEAR = 1900
        const val ONBOARDING_FIELD_COUNT = 3
        val WITHDRAW_GRACE_PERIOD: Duration = Duration.ofDays(30)
    }
}
