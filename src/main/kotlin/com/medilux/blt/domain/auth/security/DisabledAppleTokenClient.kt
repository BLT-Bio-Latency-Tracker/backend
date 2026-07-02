package com.medilux.blt.domain.auth.security

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

@Component
@ConditionalOnProperty(prefix = "blt.apple.revocation", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class DisabledAppleTokenClient : AppleTokenClient {
    override fun exchangeRefreshToken(authorizationCode: String): String? = null

    override fun revokeRefreshToken(refreshToken: String): Boolean = false
}
