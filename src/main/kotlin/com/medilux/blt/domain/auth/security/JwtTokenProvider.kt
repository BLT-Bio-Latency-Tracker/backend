package com.medilux.blt.domain.auth.security

import com.auth0.jwt.JWT
import com.auth0.jwt.JWTVerifier
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTVerificationException
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.time.Duration
import java.time.Instant
import java.util.Date
import java.util.UUID

@Component
class JwtTokenProvider(
    @Value("\${blt.jwt.secret}")
    private val secret: String,
    @Value("\${blt.jwt.issuer}")
    private val issuer: String,
    @Value("\${blt.jwt.access-token-ttl-minutes}")
    private val accessTokenTtlMinutes: Long,
    @Value("\${blt.jwt.refresh-token-ttl-days}")
    private val refreshTokenTtlDays: Long,
    @Value("\${blt.jwt.verification-token-ttl-minutes}")
    private val verificationTokenTtlMinutes: Long,
) {
    private val algorithm: Algorithm by lazy { Algorithm.HMAC256(secret) }

    val accessTokenExpiresInSeconds: Long
        get() = Duration.ofMinutes(accessTokenTtlMinutes).seconds

    val verificationTokenExpiresInSeconds: Long
        get() = Duration.ofMinutes(verificationTokenTtlMinutes).seconds

    fun issueAccessToken(userId: Long): IssuedToken = issueToken(
        subject = userId.toString(),
        tokenType = TokenType.ACCESS,
        expiresAt = Instant.now().plus(Duration.ofMinutes(accessTokenTtlMinutes)),
    )

    fun issueRefreshToken(userId: Long): IssuedToken = issueToken(
        subject = userId.toString(),
        tokenType = TokenType.REFRESH,
        expiresAt = Instant.now().plus(Duration.ofDays(refreshTokenTtlDays)),
    )

    fun issueVerificationToken(appleSubHash: String): IssuedToken {
        val now = Instant.now()
        val expiresAt = now.plus(Duration.ofMinutes(verificationTokenTtlMinutes))
        val token = JWT.create()
            .withIssuer(issuer)
            .withSubject("apple-signup")
            .withJWTId(UUID.randomUUID().toString())
            .withClaim(TOKEN_TYPE_CLAIM, TokenType.VERIFICATION.name)
            .withClaim(APPLE_SUB_HASH_CLAIM, appleSubHash)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)

        return IssuedToken(token = token, expiresAt = expiresAt)
    }

    fun getUserIdFromAccessToken(token: String): Long {
        val subject = verify(token, TokenType.ACCESS).subject
            ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)

        return subject.toLongOrNull() ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
    }

    fun getUserIdFromRefreshToken(token: String): Long {
        val subject = verify(token, TokenType.REFRESH).subject
            ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)

        return subject.toLongOrNull() ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
    }

    fun getAppleSubHashFromVerificationToken(token: String): String {
        val claim = verify(token, TokenType.VERIFICATION).getClaim(APPLE_SUB_HASH_CLAIM)
        val appleSubHash = claim.asString()

        if (appleSubHash.isNullOrBlank()) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        }

        return appleSubHash
    }

    private fun issueToken(subject: String, tokenType: TokenType, expiresAt: Instant): IssuedToken {
        val now = Instant.now()
        val token = JWT.create()
            .withIssuer(issuer)
            .withSubject(subject)
            .withJWTId(UUID.randomUUID().toString())
            .withClaim(TOKEN_TYPE_CLAIM, tokenType.name)
            .withIssuedAt(Date.from(now))
            .withExpiresAt(Date.from(expiresAt))
            .sign(algorithm)

        return IssuedToken(token = token, expiresAt = expiresAt)
    }

    private fun verify(token: String, tokenType: TokenType) = try {
        verifier(tokenType).verify(token)
    } catch (ex: JWTVerificationException) {
        throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS, cause = ex)
    } catch (ex: IllegalArgumentException) {
        throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS, cause = ex)
    }

    private fun verifier(tokenType: TokenType): JWTVerifier = JWT.require(algorithm)
        .withIssuer(issuer)
        .withClaim(TOKEN_TYPE_CLAIM, tokenType.name)
        .build()

    data class IssuedToken(val token: String, val expiresAt: Instant)

    private enum class TokenType {
        ACCESS,
        REFRESH,
        VERIFICATION,
    }

    private companion object {
        const val TOKEN_TYPE_CLAIM = "token_type"
        const val APPLE_SUB_HASH_CLAIM = "apple_sub_hash"
    }
}
