package com.medilux.blt.domain.notification.push

import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingException
import com.google.firebase.messaging.Message
import com.google.firebase.messaging.MessagingErrorCode
import com.google.firebase.messaging.Notification
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.stereotype.Component

/**
 * `blt.fcm.enabled=true` 일 때만 활성화.
 */
@Component
@ConditionalOnProperty(prefix = "blt.fcm", name = ["enabled"], havingValue = "true")
class FcmPushSender(private val firebaseMessaging: FirebaseMessaging) : PushSender {
    override fun send(token: String, message: PushMessage): PushResult {
        val fcmMessage = Message.builder()
            .setToken(token)
            .setNotification(
                Notification.builder()
                    .setTitle(message.title)
                    .setBody(message.body)
                    .build(),
            )
            .putAllData(message.data)
            .build()

        return try {
            val messageId = firebaseMessaging.send(fcmMessage)
            PushResult.success(messageId)
        } catch (ex: FirebaseMessagingException) {
            PushResult.failure(
                errorCode = ex.messagingErrorCode?.name ?: "FCM_ERROR",
                errorMessage = ex.message,
                invalidToken = ex.messagingErrorCode == MessagingErrorCode.UNREGISTERED ||
                    ex.messagingErrorCode == MessagingErrorCode.INVALID_ARGUMENT,
            )
        }
    }
}
