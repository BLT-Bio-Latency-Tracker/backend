package com.medilux.blt.domain.auth.dto

import com.medilux.blt.domain.user.entity.ConsentType

data class AppleVerifyRequest(val identityToken: String)

data class AppleSignupRequest(val verificationToken: String, val consents: List<ConsentRequest>)

data class ConsentRequest(
    val consentType: ConsentType,
    val policyVersion: String,
    val agreed: Boolean,
    val options: Map<String, Any>? = null,
)

data class AppleAuthResponse(
    val isNewUser: Boolean,
    val accessToken: String?,
    val refreshToken: String?,
    val verificationToken: String?,
    val tokenType: String?,
    val expiresInSeconds: Long?,
    val verificationExpiresInSeconds: Long?,
    val onboardingCompleted: Boolean?,
) {
    companion object {
        fun existing(session: AuthSessionResponse): AppleAuthResponse = AppleAuthResponse(
            isNewUser = false,
            accessToken = session.accessToken,
            refreshToken = session.refreshToken,
            verificationToken = null,
            tokenType = session.tokenType,
            expiresInSeconds = session.expiresInSeconds,
            verificationExpiresInSeconds = null,
            onboardingCompleted = session.onboardingCompleted,
        )

        fun newUser(verificationToken: String, verificationExpiresInSeconds: Long): AppleAuthResponse = AppleAuthResponse(
            isNewUser = true,
            accessToken = null,
            refreshToken = null,
            verificationToken = verificationToken,
            tokenType = "Bearer",
            expiresInSeconds = null,
            verificationExpiresInSeconds = verificationExpiresInSeconds,
            onboardingCompleted = null,
        )
    }
}

data class AuthSessionResponse(
    val accessToken: String,
    val refreshToken: String,
    val tokenType: String = "Bearer",
    val expiresInSeconds: Long,
    val userId: Long,
    val onboardingCompleted: Boolean,
)
