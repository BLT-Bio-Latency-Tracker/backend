package com.medilux.blt.domain.auth.security

/**
 * Apple 서버 API 연동. 계정 삭제 시 Sign in with Apple 토큰 폐기(App Store 5.1.1(v))를 위해 사용.
 */
interface AppleTokenClient {
    /** authorizationCode를 refresh_token으로 교환. 비활성/실패 시 null. */
    fun exchangeRefreshToken(authorizationCode: String): String?

    /** refresh_token 폐기(best-effort). 성공 여부 반환. */
    fun revokeRefreshToken(refreshToken: String): Boolean
}
