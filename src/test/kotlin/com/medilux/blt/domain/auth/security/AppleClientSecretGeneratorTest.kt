package com.medilux.blt.domain.auth.security

import com.auth0.jwt.JWT
import com.auth0.jwt.algorithms.Algorithm
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.security.KeyPairGenerator
import java.security.interfaces.ECPublicKey
import java.security.spec.ECGenParameterSpec
import java.util.Base64

class AppleClientSecretGeneratorTest {
    private val keyPair = KeyPairGenerator.getInstance("EC").apply {
        initialize(ECGenParameterSpec("secp256r1"))
    }.generateKeyPair()

    private fun pem(): String {
        val body = Base64.getEncoder().encodeToString(keyPair.private.encoded) // PKCS#8
        return "-----BEGIN PRIVATE KEY-----\n$body\n-----END PRIVATE KEY-----"
    }

    @Test
    fun `generates an ES256 JWT with Apple client-secret claims verifiable by the public key`() {
        val generator = AppleClientSecretGenerator(
            teamId = "R54DJK7U34",
            keyId = "YNZM3P2462",
            privateKeyPem = pem(),
            clientId = "BLT.com",
            audience = "https://appleid.apple.com",
        )

        val jwt = generator.generate()

        // 공개키로 서명 검증(변조/키 불일치 시 예외) + 클레임 확인
        val decoded = JWT.require(Algorithm.ECDSA256(keyPair.public as ECPublicKey, null))
            .withIssuer("R54DJK7U34")
            .withSubject("BLT.com")
            .withAudience("https://appleid.apple.com")
            .build()
            .verify(jwt)

        assertEquals("YNZM3P2462", decoded.keyId)
        assertEquals("ES256", decoded.algorithm)
    }
}
