package com.medilux.blt.global.config

import com.google.auth.oauth2.GoogleCredentials
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import com.google.firebase.messaging.FirebaseMessaging
import org.springframework.beans.factory.annotation.Value
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import java.io.FileInputStream

/**
 * FCM(Firebase Cloud Messaging) 초기화.
 *
 * `blt.fcm.enabled=true` 일 때만 [FirebaseMessaging] 빈을 등록한다.
 * 비활성(local/test/크레덴셜 없음) 시 빈이 없으므로 [com.medilux.blt.domain.notification.push.LoggingPushSender]
 * 가 주입되어 크레덴셜 없이도 애플리케이션이 정상 기동한다.
 */
@Configuration
@ConditionalOnProperty(prefix = "blt.fcm", name = ["enabled"], havingValue = "true")
class FcmConfig(
    @Value("\${blt.fcm.credentials-path}")
    private val credentialsPath: String,
) {
    @Bean
    fun firebaseMessaging(): FirebaseMessaging {
        val credentials = FileInputStream(credentialsPath).use { stream ->
            GoogleCredentials.fromStream(stream)
        }
        val options = FirebaseOptions.builder()
            .setCredentials(credentials)
            .build()
        val app = if (FirebaseApp.getApps().isEmpty()) {
            FirebaseApp.initializeApp(options)
        } else {
            FirebaseApp.getInstance()
        }
        return FirebaseMessaging.getInstance(app)
    }
}
