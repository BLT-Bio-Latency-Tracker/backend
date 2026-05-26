package com.medilux.blt.domain.auth.service

import com.medilux.blt.domain.auth.dto.AppleAuthResponse
import com.medilux.blt.domain.auth.dto.AppleSignupRequest
import com.medilux.blt.domain.auth.dto.AppleVerifyRequest
import com.medilux.blt.domain.auth.dto.AuthSessionResponse
import com.medilux.blt.domain.auth.entity.RefreshToken
import com.medilux.blt.domain.auth.repository.RefreshTokenRepository
import com.medilux.blt.domain.auth.security.AppleIdTokenVerifier
import com.medilux.blt.domain.auth.security.JwtTokenProvider
import com.medilux.blt.domain.user.entity.AuthType
import com.medilux.blt.domain.user.entity.ConsentLog
import com.medilux.blt.domain.user.entity.ConsentType
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserStatus
import com.medilux.blt.domain.user.repository.ConsentLogRepository
import com.medilux.blt.domain.user.repository.UserRepository
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
            return AppleAuthResponse.existing(createSession(user))
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

    private fun HttpServletRequest.clientIp(): String? {
        val forwardedFor = getHeader("X-Forwarded-For")
            ?.split(",")
            ?.firstOrNull()
            ?.trim()
            ?.takeIf { ip -> ip.isNotBlank() }

        return forwardedFor ?: remoteAddr
    }
}
