package com.medilux.blt.domain.notification.push

import org.slf4j.LoggerFactory
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * FCM 비활성(local/test) 시 사용하는 푸시 발송 구현.
 * 실제 발송 대신 로그만 남기고 성공으로 처리해 credential 없이 전체 플로우를 검증 가능.
 */
@Component
@ConditionalOnProperty(prefix = "blt.fcm", name = ["enabled"], havingValue = "false", matchIfMissing = true)
class LoggingPushSender : PushSender {
    private val log = LoggerFactory.getLogger(javaClass)

    override fun send(token: String, message: PushMessage): PushResult {
        log.info(
            "[LoggingPushSender] (FCM 비활성) push to token={} title='{}' body='{}' data={}",
            token.take(12) + "…",
            message.title,
            message.body,
            message.data,
        )
        return PushResult.success(messageId = "logged-${System.nanoTime()}")
    }
}
