package com.medilux.blt.domain.auth.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import java.security.KeyFactory
import java.security.interfaces.ECPrivateKey
import java.security.spec.PKCS8EncodedKeySpec
import java.time.Duration
import java.time.Instant
import java.util.Base64
import java.util.Date

/**
 * Apple 서버 API(token/revoke) 인증용 client_secret(ES256 JWT) 생성.
 */
class AppleClientSecretGenerator(
    private val teamId: String,
    private val keyId: String,
    privateKeyPem: String,
    private val clientId: String,
    private val audience: String,
) {
    // 서명 전용: 공개키는 null(검증은 Apple 서버가 수행).
    private val algorithm: Algorithm = Algorithm.ECDSA256(null, loadPrivateKey(privateKeyPem))

    fun generate(now: Instant = Instant.now()): String = JWT.create()
        .withKeyId(keyId)
        .withIssuer(teamId)
        .withSubject(clientId)
        .withAudience(audience)
        .withIssuedAt(Date.from(now))
        .withExpiresAt(Date.from(now.plus(CLIENT_SECRET_TTL)))
        .sign(algorithm)

    private fun loadPrivateKey(pem: String): ECPrivateKey {
        val der = Base64.getDecoder().decode(
            pem
                .replace(PEM_HEADER, "")
                .replace(PEM_FOOTER, "")
                .replace("\\s".toRegex(), ""),
        )
        return KeyFactory.getInstance("EC").generatePrivate(PKCS8EncodedKeySpec(der)) as ECPrivateKey
    }

    private companion object {
        val CLIENT_SECRET_TTL: Duration = Duration.ofMinutes(30)
        const val PEM_HEADER = "-----BEGIN PRIVATE KEY-----"
        const val PEM_FOOTER = "-----END PRIVATE KEY-----"
    }
}
