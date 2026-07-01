package com.medilux.blt.domain.auth.dto

import com.medilux.blt.domain.user.entity.ConsentType
import io.swagger.v3.oas.annotations.media.Schema
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.NotEmpty
import jakarta.validation.constraints.Size

private const val TOKEN_MAX = 4096
private const val POLICY_VERSION_MAX = 20
private const val CONSENTS_MAX = 20
private const val CONSENT_OPTIONS_MAX = 20

@Schema(description = "Apple identityToken 검증 요청")
data class AppleVerifyRequest(
    @field:Schema(
        description = "iOS Apple Sign-In 성공 후 받은 identityToken JWT",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank
    @field:Size(max = TOKEN_MAX)
    val identityToken: String,
)

@Schema(description = "Apple 신규 가입 요청")
data class AppleSignupRequest(
    @field:Schema(
        description = "Apple verify API가 신규 사용자에게 발급한 5분짜리 임시 가입 토큰",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    @field:NotBlank
    @field:Size(max = TOKEN_MAX)
    val verificationToken: String,
    @field:Schema(description = "사용자가 확인한 전체 약관 동의/비동의 목록", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:Valid
    @field:NotEmpty
    @field:Size(max = CONSENTS_MAX)
    val consents: List<ConsentRequest>,
    @field:Schema(
        description = "Apple 로그인 제공 사용자 이름을 기본 닉네임으로 저장 (선택). 미제공(null/공백) 시 서버가 랜덤 닉네임을 부여.",
        nullable = true,
    )
    @field:Size(max = 50)
    val nickname: String? = null,
)

@Schema(description = "토큰 갱신 요청")
data class RefreshTokenRequest(
    @field:Schema(description = "로그인 또는 가입 시 발급받은 refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    @field:Size(max = TOKEN_MAX)
    val refreshToken: String,
)

@Schema(description = "로그아웃 요청")
data class LogoutRequest(
    @field:Schema(description = "로그인 또는 가입 시 발급받은 refresh token", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    @field:Size(max = TOKEN_MAX)
    val refreshToken: String,
)

@Schema(description = "약관 동의 요청 항목")
data class ConsentRequest(
    @field:Schema(
        description = "약관 종류. TERMS_OF_SERVICE, PRIVACY_POLICY, HEALTH_DATA는 필수 동의입니다.",
        example = "TERMS_OF_SERVICE",
        requiredMode = Schema.RequiredMode.REQUIRED,
    )
    val consentType: ConsentType,
    @field:Schema(description = "약관 버전", example = "1.0", requiredMode = Schema.RequiredMode.REQUIRED)
    @field:NotBlank
    @field:Size(max = POLICY_VERSION_MAX)
    val policyVersion: String,
    @field:Schema(description = "사용자 동의 여부. 필수 약관은 true여야 가입 가능.", example = "true", requiredMode = Schema.RequiredMode.REQUIRED)
    val agreed: Boolean,
    @field:Schema(description = "약관별 하위 옵션. MVP에서는 선택 입력값입니다.", example = """{"push":false}""")
    @field:Size(max = CONSENT_OPTIONS_MAX)
    val options: Map<String, Any>? = null,
)

@Schema(description = "Apple verify API 응답")
data class AppleAuthResponse(
    @field:Schema(description = "신규 사용자 여부. true면 signup API로 약관 동의를 완료해야 합니다.", example = "true")
    val isNewUser: Boolean,
    @field:Schema(description = "기존 회원일 때 발급되는 access token. 신규 회원이면 null입니다.")
    val accessToken: String?,
    @field:Schema(description = "기존 회원일 때 발급되는 refresh token. 신규 회원이면 null입니다.")
    val refreshToken: String?,
    @field:Schema(description = "신규 회원일 때 발급되는 임시 가입 토큰. 기존 회원이면 null입니다.")
    val verificationToken: String?,
    @field:Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String?,
    @field:Schema(description = "access token 만료까지 남은 초. 신규 회원이면 null입니다.", example = "3600")
    val expiresInSeconds: Long?,
    @field:Schema(description = "verificationToken 만료까지 남은 초. 기존 회원이면 null입니다.", example = "300")
    val verificationExpiresInSeconds: Long?,
    @field:Schema(description = "기존 회원의 온보딩 완료 여부. 신규 회원이면 null입니다.", example = "false")
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

@Schema(description = "정식 로그인 세션 응답")
data class AuthSessionResponse(
    @field:Schema(description = "API 인증에 사용할 access token")
    val accessToken: String,
    @field:Schema(description = "access token 재발급에 사용할 refresh token")
    val refreshToken: String,
    @field:Schema(description = "토큰 타입", example = "Bearer")
    val tokenType: String = "Bearer",
    @field:Schema(description = "access token 만료까지 남은 초", example = "3600")
    val expiresInSeconds: Long,
    @field:Schema(description = "가입/로그인된 사용자 ID", example = "1")
    val userId: Long,
    @field:Schema(description = "온보딩 완료 여부", example = "false")
    val onboardingCompleted: Boolean,
)
