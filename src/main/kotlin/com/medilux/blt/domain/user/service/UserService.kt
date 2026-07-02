package com.medilux.blt.domain.user.service

import com.medilux.blt.domain.auth.repository.RefreshTokenRepository
import com.medilux.blt.domain.auth.security.AppleTokenCipher
import com.medilux.blt.domain.auth.security.AppleTokenClient
import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.user.dto.OnboardingRequest
import com.medilux.blt.domain.user.dto.OnboardingResponse
import com.medilux.blt.domain.user.dto.ProfileUpdateRequest
import com.medilux.blt.domain.user.dto.TermsHistoryItemResponse
import com.medilux.blt.domain.user.dto.UserResponse
import com.medilux.blt.domain.user.dto.WithdrawResponse
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserStatus
import com.medilux.blt.domain.user.repository.ConsentLogRepository
import com.medilux.blt.domain.user.repository.UserDeviceRepository
import com.medilux.blt.domain.user.repository.UserRepository
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import io.sentry.Sentry
import org.slf4j.LoggerFactory
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
    private val consentLogRepository: ConsentLogRepository,
    private val appleTokenClient: AppleTokenClient,
    private val appleTokenCipher: AppleTokenCipher,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    /** 약관 동의 이력 조회 — append-only 전체 이력을 최신순으로 반환. */
    @Transactional(readOnly = true)
    fun getTermsHistory(userId: Long): List<TermsHistoryItemResponse> = consentLogRepository.findByUserIdOrderByAgreedAtDescIdDesc(userId)
        .map(TermsHistoryItemResponse::from)

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

        request.nickname?.trim()?.takeIf(String::isNotBlank)?.let { nickname -> user.nickname = nickname }
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
        revokeAppleTokenBestEffort(user)

        return WithdrawResponse(
            status = user.status.name,
            withdrawnAt = now,
            withdrawScheduledAt = now.plus(WITHDRAW_GRACE_PERIOD),
        )
    }

    /**
     * 계정 삭제 시 Apple에 Sign in with Apple 토큰 폐기(revoke)를 요청(App Store 5.1.1(v)).
     * best-effort — 외부 호출 실패가 탈퇴를 막지 않도록 로깅(Sentry) 후 진행하고, 저장된 토큰은 제거한다.
     * (revocation 비활성 환경에서는 no-op 클라이언트라 호출이 무해하다.)
     */
    private fun revokeAppleTokenBestEffort(user: User) {
        val encrypted = user.appleRefreshToken ?: return
        runCatching { appleTokenClient.revokeRefreshToken(appleTokenCipher.decrypt(encrypted)) }
            .onFailure { ex ->
                log.warn("Apple 토큰 폐기 실패 (userId={}) — 탈퇴는 계속 진행", user.id, ex)
                Sentry.captureException(ex)
            }
        user.appleRefreshToken = null
    }

    private companion object {
        const val MIN_BIRTH_YEAR = 1900
        const val ONBOARDING_FIELD_COUNT = 3
        val WITHDRAW_GRACE_PERIOD: Duration = Duration.ofDays(30)
    }
}
