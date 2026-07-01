package com.medilux.blt.domain.notification.service

import com.medilux.blt.domain.notification.dto.NotificationSettingsUpdateRequest
import com.medilux.blt.domain.user.entity.AuthType
import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.repository.UserRepository
import io.mockk.every
import io.mockk.mockk
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.time.LocalTime
import java.util.Optional

class NotificationSettingsServiceTest {
    private val userRepository = mockk<UserRepository>()
    private val service = NotificationSettingsService(userRepository)

    private fun user() = User(appleSubHash = "hash", authType = AuthType.APPLE)

    /**
     * 리마인더 시각은 분 단위로 절삭 저장돼야 한다. 디스패치(ReminderDispatchService)가
     * 매 분 truncatedTo(MINUTES) 시각과 동등 비교하므로, 초/나노가 남으면 알림이 영구 미발화된다.
     */
    @Test
    fun `update truncates reminder times to minute precision`() {
        val user = user()
        every { userRepository.findById(1L) } returns Optional.of(user)

        service.update(
            userId = 1L,
            request = NotificationSettingsUpdateRequest(
                pvtReminderTime = LocalTime.of(7, 30, 45, 123_000_000),
                sleepReminderTime = LocalTime.of(22, 15, 59),
            ),
        )

        assertEquals(LocalTime.of(7, 30), user.pvtReminderTime)
        assertEquals(LocalTime.of(22, 15), user.sleepReminderTime)
    }

    @Test
    fun `update keeps already minute-aligned times unchanged`() {
        val user = user()
        every { userRepository.findById(1L) } returns Optional.of(user)

        service.update(
            userId = 1L,
            request = NotificationSettingsUpdateRequest(pvtReminderTime = LocalTime.of(6, 0)),
        )

        assertEquals(LocalTime.of(6, 0), user.pvtReminderTime)
    }
}
