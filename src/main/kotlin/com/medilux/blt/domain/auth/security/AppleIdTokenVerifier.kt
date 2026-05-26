package com.medilux.blt.domain.auth.security

import com.auth0.jwk.JwkException
import com.auth0.jwk.JwkProviderBuilder
import com.auth0.jwk.NetworkException
import com.auth0.jwk.SigningKeyNotFoundException
import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import com.auth0.jwt.exceptions.JWTDecodeException
import com.auth0.jwt.exceptions.JWTVerificationException
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Component
import java.net.URI
import java.security.interfaces.RSAPublicKey
import java.util.concurrent.TimeUnit

@Component
class AppleIdTokenVerifier(
    @Value("\${blt.apple.issuer}")
    private val issuer: String,
    @Value("\${blt.apple.client-id}")
    private val clientId: String,
) {
    private val jwkProvider = JwkProviderBuilder(URI.create("$issuer/auth/keys").toURL())
        .cached(10, 24, TimeUnit.HOURS)
        .rateLimited(10, 1, TimeUnit.MINUTES)
        .build()

    fun verifyAndGetSubject(identityToken: String): String {
        val decodedJwt = try {
            JWT.decode(identityToken)
        } catch (ex: JWTDecodeException) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS, cause = ex)
        }

        val publicKey = try {
            jwkProvider.get(decodedJwt.keyId).publicKey as? RSAPublicKey
                ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        } catch (ex: NetworkException) {
            throw BltException(ErrorCode.SERVICE_UNAVAILABLE, cause = ex)
        } catch (ex: SigningKeyNotFoundException) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS, cause = ex)
        } catch (ex: JwkException) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS, cause = ex)
        }

        return try {
            val verifiedJwt = JWT.require(Algorithm.RSA256(publicKey, null))
                .withIssuer(issuer)
                .withAudience(clientId)
                .build()
                .verify(identityToken)

            verifiedJwt.subject ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
        } catch (ex: JWTVerificationException) {
            throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS, cause = ex)
        }
    }
}
