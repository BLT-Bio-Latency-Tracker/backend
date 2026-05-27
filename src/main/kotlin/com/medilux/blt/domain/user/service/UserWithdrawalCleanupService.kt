package com.medilux.blt.domain.user.service

import com.medilux.blt.domain.auth.repository.RefreshTokenRepository
import com.medilux.blt.domain.roi.repository.RecommendationRepository
import com.medilux.blt.domain.sleep.repository.SleepRecordRepository
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserStatus
import com.medilux.blt.domain.user.repository.ConsentLogRepository
import com.medilux.blt.domain.user.repository.NotificationLogRepository
import com.medilux.blt.domain.user.repository.UserDeviceRepository
import com.medilux.blt.domain.user.repository.UserRepository
import com.medilux.blt.global.common.util.HashUtils
import org.springframework.stereotype.Service
import org.springframework.transaction.support.TransactionTemplate
import java.time.Duration
import java.time.Instant
import java.util.UUID

@Service
class UserWithdrawalCleanupService(
    private val userRepository: UserRepository,
    private val refreshTokenRepository: RefreshTokenRepository,
    private val userDeviceRepository: UserDeviceRepository,
    private val notificationLogRepository: NotificationLogRepository,
    private val consentLogRepository: ConsentLogRepository,
    private val sleepRecordRepository: SleepRecordRepository,
    private val recommendationRepository: RecommendationRepository,
    private val transactionTemplate: TransactionTemplate,
) {
    fun cleanupExpiredWithdrawals(now: Instant = Instant.now()): Int {
        val cutoff = now.minus(WITHDRAW_GRACE_PERIOD)
        var cleanedCount = 0

        while (true) {
            val users = userRepository.findTop100ByStatusAndWithdrawnAtLessThanEqualOrderByIdAsc(
                status = UserStatus.WITHDRAW_PENDING,
                withdrawnAt = cutoff,
            )
            if (users.isEmpty()) {
                return cleanedCount
            }

            users.map { user -> user.id }.forEach { userId ->
                val cleaned = transactionTemplate.execute {
                    cleanupUserById(userId, cutoff, now)
                } ?: false

                if (cleaned) {
                    cleanedCount += 1
                }
            }
        }
    }

    private fun cleanupUserById(userId: Long, cutoff: Instant, now: Instant): Boolean {
        val user = userRepository.findById(userId).orElse(null) ?: return false
        val withdrawnAt = user.withdrawnAt ?: return false

        if (user.status != UserStatus.WITHDRAW_PENDING || withdrawnAt > cutoff) {
            return false
        }

        cleanupUser(user, now)
        return true
    }

    private fun cleanupUser(user: User, now: Instant) {
        val userId = user.id

        notificationLogRepository.deleteAllByUserId(userId)
        refreshTokenRepository.deleteAllByUserId(userId)
        userDeviceRepository.deleteAllByUserId(userId)
        recommendationRepository.deleteAllByUserId(userId)
        consentLogRepository.clearClientIpByUserId(userId)
        sleepRecordRepository.clearRawPayloadByUserId(userId)

        user.appleSubHash = withdrawnAppleSubHash(userId, now)
        user.email = null
        user.nickname = null
        user.occupation = null
        user.apnsDeviceToken = null
        user.notificationEnabled = false
        user.customNotificationOptions = emptyMap()
        user.notificationTimezone = DEFAULT_NOTIFICATION_TIMEZONE
        user.status = UserStatus.WITHDRAWN
        user.delete(now)
    }

    private fun withdrawnAppleSubHash(userId: Long, now: Instant): String =
        HashUtils.sha256Hex("withdrawn:$userId:${now.toEpochMilli()}:${UUID.randomUUID()}")

    private companion object {
        const val DEFAULT_NOTIFICATION_TIMEZONE = "Asia/Seoul"
        val WITHDRAW_GRACE_PERIOD: Duration = Duration.ofDays(30)
    }
}
