package com.medilux.blt.domain.auth.service

import com.medilux.blt.domain.auth.dto.AppleAuthResponse
import com.medilux.blt.domain.auth.dto.AppleSignupRequest
import com.medilux.blt.domain.auth.dto.AppleVerifyRequest
import com.medilux.blt.domain.auth.dto.AuthSessionResponse
import com.medilux.blt.domain.auth.dto.LogoutRequest
import com.medilux.blt.domain.auth.dto.RefreshTokenRequest
import com.medilux.blt.domain.auth.entity.RefreshToken
import com.medilux.blt.domain.auth.repository.RefreshTokenRepository
import com.medilux.blt.domain.auth.security.AppleIdTokenVerifier
import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.auth.security.JwtTokenProvider
import com.medilux.blt.domain.user.entity.AuthType
import com.medilux.blt.domain.user.entity.ConsentLog
import com.medilux.blt.domain.user.entity.ConsentType
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserStatus
import com.medilux.blt.domain.user.repository.ConsentLogRepository
import com.medilux.blt.domain.user.repository.UserRepository
import com.medilux.blt.domain.user.service.UserWithdrawalCleanupService
import com.medilux.blt.global.common.util.HashUtils
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import jakarta.servlet.http.HttpServletRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class AuthService(
    private val appleIdTokenVerifier: AppleIdTokenVerifier,
    private val jwtTokenProvider: JwtTokenProvider,
    private val userRepository: UserRepository,
    private val consentLogRepository: ConsentLogRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userWithdrawalCleanupService: UserWithdrawalCleanupService,
) {
    @Transactional
    fun verifyApple(request: AppleVerifyRequest): AppleAuthResponse {
        if (request.identityToken.isBlank()) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val appleSub = appleIdTokenVerifier.verifyAndGetSubject(request.identityToken)
        val appleSubHash = HashUtils.sha256Hex(appleSub)
        val user = userRepository.findByAppleSubHash(appleSubHash)

        if (user != null) {
            if (user.status != UserStatus.WITHDRAW_PENDING) {
                return AppleAuthResponse.existing(createSession(user))
            }

            // 탈퇴 유예 중 동일 Apple ID 재로그인: 자동 복구하지 않고(복구 불가 정책) 기존 계정을 즉시 익명화(파기)한 뒤 아래 신규 가입 경로로 진행한다.
            // 익명화 시 appleSubHash가 재생성되고 soft delete 되므로, 신규 가입은 새 계정으로 생성된다.
            userWithdrawalCleanupService.anonymizeImmediately(user)
        }

        val verificationToken = jwtTokenProvider.issueVerificationToken(appleSubHash)
        return AppleAuthResponse.newUser(
            verificationToken = verificationToken.token,
            verificationExpiresInSeconds = jwtTokenProvider.verificationTokenExpiresInSeconds,
        )
    }

    @Transactional
    fun signup(request: AppleSignupRequest, httpServletRequest: HttpServletRequest): AuthSessionResponse {
        if (request.verificationToken.isBlank()) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val appleSubHash = jwtTokenProvider.getAppleSubHashFromVerificationToken(request.verificationToken)
        if (userRepository.existsByAppleSubHash(appleSubHash)) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        validateConsents(request)

        val user = userRepository.save(
            User(
                appleSubHash = appleSubHash,
                authType = AuthType.APPLE,
                status = UserStatus.ACTIVE,
            ),
        )
        val now = Instant.now()
        val clientIp = httpServletRequest.clientIp()
        val consentLogs = request.consents.map { consent ->
            ConsentLog(
                user = user,
                consentType = consent.consentType,
                policyVersion = consent.policyVersion.trim(),
                agreed = consent.agreed,
                agreedAt = now,
                clientIp = clientIp,
            )
        }

        consentLogRepository.saveAll(consentLogs)

        return createSession(user)
    }

    @Transactional
    fun refreshSession(request: RefreshTokenRequest): AuthSessionResponse {
        if (request.refreshToken.isBlank()) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val userId = jwtTokenProvider.getUserIdFromRefreshToken(request.refreshToken)
        val tokenHash = HashUtils.sha256Hex(request.refreshToken)
        val savedRefreshToken = refreshTokenRepository.findByTokenHash(tokenHash)
            ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)

        if (savedRefreshToken.user.id != userId) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val revokedCount = refreshTokenRepository.revokeActiveToken(
            tokenHash = tokenHash,
            revokedAt = Instant.now(),
        )
        if (revokedCount != 1) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        return createSession(savedRefreshToken.user)
    }

    @Transactional
    fun logout(principal: AuthUserPrincipal?, request: LogoutRequest) {
        val principalUserId = principal?.userId ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        if (request.refreshToken.isBlank()) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val tokenUserId = jwtTokenProvider.getUserIdFromRefreshToken(request.refreshToken)
        if (tokenUserId != principalUserId) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        val tokenHash = HashUtils.sha256Hex(request.refreshToken)
        refreshTokenRepository.revokeActiveToken(tokenHash, Instant.now())
    }

    private fun validateConsents(request: AppleSignupRequest) {
        if (request.consents.any { consent -> consent.policyVersion.isBlank() }) {
            throw BltException(ErrorCode.VALIDATION_FAILED)
        }

        val agreedTypes = request.consents
            .filter { consent -> consent.agreed }
            .map { consent -> consent.consentType }
            .toSet()

        val requiredTypes = ConsentType.entries
            .filter { consentType -> consentType.required }
            .toSet()

        if (!agreedTypes.containsAll(requiredTypes)) {
            throw BltException(ErrorCode.TERMS_REQUIRED_NOT_AGREED)
        }
    }

    private fun createSession(user: User): AuthSessionResponse {
        val accessToken = jwtTokenProvider.issueAccessToken(user.id)
        val refreshToken = jwtTokenProvider.issueRefreshToken(user.id)

        refreshTokenRepository.save(
            RefreshToken(
                user = user,
                tokenHash = HashUtils.sha256Hex(refreshToken.token),
                expiresAt = refreshToken.expiresAt,
            ),
        )

        return AuthSessionResponse(
            accessToken = accessToken.token,
            refreshToken = refreshToken.token,
            expiresInSeconds = jwtTokenProvider.accessTokenExpiresInSeconds,
            userId = user.id,
            onboardingCompleted = user.onboardingCompleted,
        )
    }

    private fun HttpServletRequest.clientIp(): String? = remoteAddr
}
