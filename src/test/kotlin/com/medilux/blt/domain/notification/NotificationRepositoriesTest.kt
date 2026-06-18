package com.medilux.blt.domain.notification

import com.medilux.blt.domain.user.entity.AuthType
import com.medilux.blt.domain.user.entity.DevicePlatform
import com.medilux.blt.domain.user.entity.NotificationLog
import com.medilux.blt.domain.user.entity.NotificationStatus
import com.medilux.blt.domain.user.entity.NotificationType
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserDevice
import com.medilux.blt.domain.user.repository.NotificationLogRepository
import com.medilux.blt.domain.user.repository.UserDeviceRepository
import com.medilux.blt.domain.user.repository.UserRepository
import org.assertj.core.api.Assertions.assertThat
import org.junit.jupiter.api.Test
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.jdbc.AutoConfigureTestDatabase
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager
import org.springframework.boot.testcontainers.service.connection.ServiceConnection
import org.springframework.data.domain.PageRequest
import org.testcontainers.containers.PostgreSQLContainer
import org.testcontainers.junit.jupiter.Container
import org.testcontainers.junit.jupiter.Testcontainers
import java.time.Instant
import java.time.LocalTime

@Testcontainers
@DataJpaTest(properties = ["spring.jpa.hibernate.ddl-auto=create"])
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NotificationRepositoriesTest {
    @Autowired
    lateinit var entityManager: TestEntityManager

    @Autowired
    lateinit var userRepository: UserRepository

    @Autowired
    lateinit var userDeviceRepository: UserDeviceRepository

    @Autowired
    lateinit var notificationLogRepository: NotificationLogRepository

    private val pvtTime: LocalTime = LocalTime.of(7, 30)

    private fun persistUser(
        appleSubHash: String,
        enabled: Boolean = true,
        zone: String = "Asia/Seoul",
        pvtReminderTime: LocalTime = pvtTime,
    ): User = entityManager.persist(
        User(
            appleSubHash = appleSubHash,
            authType = AuthType.APPLE,
            notificationEnabled = enabled,
            notificationTimezone = zone,
            pvtReminderTime = pvtReminderTime,
        ),
    )

    private fun persistActiveDevice(user: User, token: String) {
        entityManager.persist(
            UserDevice(
                user = user,
                fcmToken = token,
                platform = DevicePlatform.IOS,
                lastActiveAt = Instant.now(),
            ),
        )
    }

    @Test
    fun `findActiveUsersForPvtReminder returns only enabled users with active device at matching time`() {
        val target = persistUser("hash-target").also { persistActiveDevice(it, "token-target") }
        persistUser("hash-no-device") // 디바이스 없음 → 제외
        persistUser("hash-disabled", enabled = false).also { persistActiveDevice(it, "token-disabled") } // 비활성 → 제외
        persistUser("hash-other-time", pvtReminderTime = LocalTime.of(9, 0))
            .also { persistActiveDevice(it, "token-other-time") } // 시각 불일치 → 제외
        entityManager.flush()
        entityManager.clear()

        val result = userRepository.findActiveUsersForPvtReminder("Asia/Seoul", pvtTime, PageRequest.of(0, 100))

        assertThat(result.map { it.id }).containsExactly(target.id)
    }

    @Test
    fun `revoked device is not active and excludes user from reminder`() {
        val user = persistUser("hash-revoked")
        val device = UserDevice(
            user = user,
            fcmToken = "token-revoked",
            platform = DevicePlatform.IOS,
            lastActiveAt = Instant.now(),
        ).apply { revokedAt = Instant.now() }
        entityManager.persist(device)
        entityManager.flush()
        entityManager.clear()

        val result = userRepository.findActiveUsersForPvtReminder("Asia/Seoul", pvtTime, PageRequest.of(0, 100))

        assertThat(result).isEmpty()
        assertThat(userDeviceRepository.findByUserIdAndRevokedAtIsNull(user.id)).isEmpty()
    }

    @Test
    fun `dedupe existsBy detects same-day reminder`() {
        val user = persistUser("hash-dedupe")
        val now = Instant.now()
        entityManager.persist(
            NotificationLog(
                user = user,
                notificationType = NotificationType.PVT_REMINDER,
                title = "t",
                body = "b",
                status = NotificationStatus.SENT,
                scheduledAt = now,
            ),
        )
        entityManager.flush()

        val exists = notificationLogRepository
            .existsByUserIdAndNotificationTypeAndScheduledAtGreaterThanEqual(
                user.id,
                NotificationType.PVT_REMINDER,
                now.minusSeconds(60),
            )
        val existsOtherType = notificationLogRepository
            .existsByUserIdAndNotificationTypeAndScheduledAtGreaterThanEqual(
                user.id,
                NotificationType.SLEEP_REMINDER,
                now.minusSeconds(60),
            )

        assertThat(exists).isTrue()
        assertThat(existsOtherType).isFalse()
    }

    @Test
    fun `markAllReadByUserId sets openedAt for unread notifications`() {
        val user = persistUser("hash-read")
        entityManager.persist(
            NotificationLog(
                user = user,
                notificationType = NotificationType.PVT_REMINDER,
                title = "t",
                body = "b",
                status = NotificationStatus.SENT,
                scheduledAt = Instant.now(),
            ),
        )
        entityManager.flush()

        val updated = notificationLogRepository.markAllReadByUserId(user.id, Instant.now())
        entityManager.clear()

        assertThat(updated).isEqualTo(1)
        val reloaded = notificationLogRepository.findByUserIdOrderByScheduledAtDesc(user.id, PageRequest.of(0, 10))
        assertThat(reloaded.content).hasSize(1)
        assertThat(reloaded.content.first().openedAt).isNotNull()
        assertThat(reloaded.content.first().status).isEqualTo(NotificationStatus.OPENED)
    }

    @Test
    fun `device upsert lookup finds by user and token`() {
        val user = persistUser("hash-device")
        persistActiveDevice(user, "token-abc")
        entityManager.flush()
        entityManager.clear()

        assertThat(userDeviceRepository.findByUserIdAndFcmToken(user.id, "token-abc")).isNotNull()
        assertThat(userDeviceRepository.findByUserIdAndFcmToken(user.id, "nope")).isNull()
    }

    companion object {
        @Container
        @ServiceConnection
        @JvmStatic
        val postgres = PostgreSQLContainer("postgres:16")
    }
}
