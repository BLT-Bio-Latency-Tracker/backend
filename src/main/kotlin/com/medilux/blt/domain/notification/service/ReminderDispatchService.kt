package com.medilux.blt.domain.notification.service

import com.medilux.blt.domain.notification.push.PushMessage
import com.medilux.blt.domain.notification.push.PushSender
import com.medilux.blt.domain.user.entity.NotificationLog
import com.medilux.blt.domain.user.entity.NotificationStatus
import com.medilux.blt.domain.user.entity.NotificationType
import com.medilux.blt.domain.user.repository.NotificationLogRepository
import com.medilux.blt.domain.user.repository.UserDeviceRepository
import com.medilux.blt.domain.user.repository.UserRepository
import org.slf4j.LoggerFactory
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Instant
import java.time.LocalTime
import java.time.ZoneId
import java.time.temporal.ChronoUnit

/**
 * 데일리 리마인더 디스패치. 매 분 호출되어, 사용자별 타임존 기준 현재 시각이
 * 측정/취침 알림 시각과 일치하는 활성(active device 보유) 사용자에게 푸시를 발송한다.
 *
 * - 중복 방지: 같은 (user, type)에 대해 오늘(로컬) 이미 기록이 있으면 skip.
 * - 무효 토큰: 발송 실패 시 해당 디바이스 revoke.
 * - 사용자별 독립 트랜잭션([TransactionTemplate])으로 한 사용자 실패가 전체를 막지 않는다.
 *
 * MVP는 스케줄러 단일 인스턴스를 가정한다(수평 확장 시 ShedLock/유니크 인덱스로 중복 발송 차단 필요).
 */
@Service
class ReminderDispatchService(
    private val userRepository: UserRepository,
    private val userDeviceRepository: UserDeviceRepository,
    private val notificationLogRepository: NotificationLogRepository,
    private val pushSender: PushSender,
    private val transactionTemplate: TransactionTemplate,
) {
    private val log = LoggerFactory.getLogger(javaClass)

    fun dispatchDueReminders(now: Instant = Instant.now()) {
        val zones = userRepository.findDistinctNotificationTimezones()
        for (zoneId in zones) {
            val zone = runCatching { ZoneId.of(zoneId) }.getOrNull()
            if (zone == null) {
                log.warn("알 수 없는 알림 타임존: {} — 건너뜀", zoneId)
                continue
            }
            val localTime = now.atZone(zone).toLocalTime().truncatedTo(ChronoUnit.MINUTES)
            dispatchForType(NotificationType.PVT_REMINDER, zoneId, zone, localTime, now)
            dispatchForType(NotificationType.SLEEP_REMINDER, zoneId, zone, localTime, now)
        }
    }

    private fun dispatchForType(type: NotificationType, zoneId: String, zone: ZoneId, localTime: LocalTime, now: Instant) {
        val pageable = PageRequest.of(0, MAX_USERS_PER_MINUTE)
        val users = when (type) {
            NotificationType.PVT_REMINDER -> userRepository.findActiveUsersForPvtReminder(zoneId, localTime, pageable)
            NotificationType.SLEEP_REMINDER -> userRepository.findActiveUsersForSleepReminder(zoneId, localTime, pageable)
            else -> emptyList()
        }
        if (users.isEmpty()) {
            return
        }
        if (users.size == MAX_USERS_PER_MINUTE) {
            log.warn("리마인더 대상 사용자가 분당 상한({})에 도달 — 일부 누락 가능. type={}", MAX_USERS_PER_MINUTE, type)
        }

        val startOfDay = localTime.let { now.atZone(zone).toLocalDate().atStartOfDay(zone).toInstant() }
        users.forEach { user ->
            val userId = user.id
            transactionTemplate.execute { sendReminder(userId, type, startOfDay, now) }
        }
    }

    private fun sendReminder(userId: Long, type: NotificationType, startOfDay: Instant, now: Instant) {
        // 오늘(로컬) 이미 동일 알림이 있으면 중복 발송하지 않는다.
        if (notificationLogRepository
                .existsByUserIdAndNotificationTypeAndScheduledAtGreaterThanEqual(userId, type, startOfDay)
        ) {
            return
        }

        val devices = userDeviceRepository.findByUserIdAndRevokedAtIsNull(userId)
        if (devices.isEmpty()) {
            return
        }
        val user = userRepository.findById(userId).orElse(null) ?: return

        val (title, body) = copyFor(type)
        val payload = mapOf<String, Any?>("type" to type.name)

        var firstMessageId: String? = null
        var anySuccess = false
        var lastErrorCode: String? = null
        var lastErrorMessage: String? = null

        devices.forEach { device ->
            val result = pushSender.send(
                token = device.fcmToken,
                message = PushMessage(title = title, body = body, data = mapOf("type" to type.name)),
            )
            if (result.success) {
                anySuccess = true
                if (firstMessageId == null) {
                    firstMessageId = result.messageId
                }
            } else {
                lastErrorCode = result.errorCode
                lastErrorMessage = result.errorMessage
                if (result.invalidToken) {
                    device.revokedAt = now
                }
            }
        }

        val notificationLog = NotificationLog(
            user = user,
            userDevice = devices.firstOrNull(),
            notificationType = type,
            title = title,
            body = body,
            payload = payload,
            fcmMessageId = firstMessageId,
            status = if (anySuccess) NotificationStatus.SENT else NotificationStatus.FAILED,
            errorCode = if (anySuccess) null else lastErrorCode,
            errorMessage = if (anySuccess) null else lastErrorMessage,
            scheduledAt = now,
            sentAt = if (anySuccess) now else null,
        )
        notificationLogRepository.save(notificationLog)
    }

    private fun copyFor(type: NotificationType): Pair<String, String> = when (type) {
        NotificationType.PVT_REMINDER ->
            "🌅 아침 측정 알림" to "좋은 아침이에요! 오늘의 컨디션을 측정해보세요"
        NotificationType.SLEEP_REMINDER ->
            "🌙 취침 알림" to "권장 취침 시각이에요. 충분한 수면이 내일의 컨디션을 좌우해요"
        else -> "BLT 알림" to ""
    }

    private companion object {
        const val MAX_USERS_PER_MINUTE = 1000
    }
}
