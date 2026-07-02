package com.medilux.blt.domain.auth.security

import com.fasterxml.jackson.annotation.JsonProperty
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.http.MediaType
import org.springframework.stereotype.Component
import org.springframework.util.LinkedMultiValueMap
import org.springframework.web.client.RestClient

@Component
@ConditionalOnProperty(prefix = "blt.apple.revocation", name = ["enabled"], havingValue = "true")
class AppleTokenHttpClient(
    @Value("\${blt.apple.client-id}")
    private val clientId: String,
    @Value("\${blt.apple.issuer}")
    private val issuer: String,
    @Value("\${blt.apple.revocation.team-id}")
    teamId: String,
    @Value("\${blt.apple.revocation.key-id}")
    keyId: String,
    @Value("\${blt.apple.revocation.private-key}")
    privateKey: String,
) : AppleTokenClient {
    private val log = LoggerFactory.getLogger(javaClass)
    private val clientSecretGenerator =
        AppleClientSecretGenerator(teamId = teamId, keyId = keyId, privateKeyPem = privateKey, clientId = clientId, audience = issuer)
    private val restClient = RestClient.builder().baseUrl(issuer).build()

    override fun exchangeRefreshToken(authorizationCode: String): String? = runCatching {
        val body = form().apply {
            add("code", authorizationCode)
            add("grant_type", "authorization_code")
        }
        restClient.post()
            .uri("/auth/token")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .body(AppleTokenResponse::class.java)
            ?.refreshToken
    }.getOrElse { ex ->
        log.warn("Apple authorizationCode 교환 실패", ex)
        null
    }

    override fun revokeRefreshToken(refreshToken: String): Boolean = runCatching {
        val body = form().apply {
            add("token", refreshToken)
            add("token_type_hint", "refresh_token")
        }
        restClient.post()
            .uri("/auth/revoke")
            .contentType(MediaType.APPLICATION_FORM_URLENCODED)
            .body(body)
            .retrieve()
            .toBodilessEntity()
        true
    }.getOrElse { ex ->
        log.warn("Apple refresh token revoke 실패", ex)
        false
    }

    private fun form(): LinkedMultiValueMap<String, String> = LinkedMultiValueMap<String, String>().apply {
        add("client_id", clientId)
        add("client_secret", clientSecretGenerator.generate())
    }

    private data class AppleTokenResponse(@JsonProperty("refresh_token") val refreshToken: String? = null)
}
