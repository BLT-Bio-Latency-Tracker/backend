package com.medilux.blt.domain.notification.push

/**
 * 구현체는 FCM 활성 여부에 따라 단 하나만 주입([FcmPushSender] 또는 [LoggingPushSender]).
 * iOS는 FCM → APNs 경유, Android는 FCM 네이티브로 동일 경로를 사용.
 */
interface PushSender {
    fun send(token: String, message: PushMessage): PushResult
}

data class PushMessage(val title: String, val body: String, val data: Map<String, String> = emptyMap())

data class PushResult(
    val success: Boolean,
    val messageId: String? = null,
    val errorCode: String? = null,
    val errorMessage: String? = null,
    /** 토큰이 더 이상 유효하지 않아 디바이스를 revoke 해야 하는 경우 true. */
    val invalidToken: Boolean = false,
) {
    companion object {
        fun success(messageId: String): PushResult = PushResult(success = true, messageId = messageId)

        fun failure(errorCode: String, errorMessage: String?, invalidToken: Boolean = false): PushResult = PushResult(
            success = false,
            errorCode = errorCode,
            errorMessage = errorMessage,
            invalidToken = invalidToken,
        )
    }
}
