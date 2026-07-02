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

    /**
     * 탈퇴 유예 중인 사용자를 30일 대기 없이 즉시 익명화(파기).
     *
     * 탈퇴 유예 중 동일 Apple ID 재로그인 시, 자동 복구 대신 기존 계정을 파기하고
     * 신규 가입으로 진행하기 위해 사용한다. (복구 불가 정책)
     */
    fun anonymizeImmediately(user: User, now: Instant = Instant.now()) {
        check(user.status == UserStatus.WITHDRAW_PENDING) {
            "즉시 익명화는 WITHDRAW_PENDING 상태에서만 가능합니다 (현재: ${user.status})"
        }
        cleanupUser(user, now)
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
        user.appleRefreshToken = null
        user.email = null
        user.nickname = null
        user.occupation = null
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
