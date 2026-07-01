package com.medilux.blt.domain.notification.service

import com.medilux.blt.domain.notification.dto.NotificationSettingsResponse
import com.medilux.blt.domain.notification.dto.NotificationSettingsUpdateRequest
import com.medilux.blt.domain.user.repository.UserRepository
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.DateTimeException
import java.time.ZoneId
import java.time.temporal.ChronoUnit

@Service
class NotificationSettingsService(private val userRepository: UserRepository) {
    @Transactional(readOnly = true)
    fun get(userId: Long): NotificationSettingsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }
        return NotificationSettingsResponse.from(user)
    }

    @Transactional
    fun update(userId: Long, request: NotificationSettingsUpdateRequest): NotificationSettingsResponse {
        val user = userRepository.findById(userId)
            .orElseThrow { BltException(ErrorCode.USER_NOT_FOUND) }

        request.notificationEnabled?.let { user.notificationEnabled = it }
        request.pvtReminderTime?.let { user.pvtReminderTime = it.truncatedTo(ChronoUnit.MINUTES) }
        request.sleepReminderTime?.let { user.sleepReminderTime = it.truncatedTo(ChronoUnit.MINUTES) }
        request.notificationTimezone?.let { zone ->
            user.notificationTimezone = validateZone(zone)
        }
        request.customNotificationOptions?.let { user.customNotificationOptions = it }

        return NotificationSettingsResponse.from(user)
    }

    private fun validateZone(zone: String): String = try {
        ZoneId.of(zone).id
    } catch (ex: DateTimeException) {
        throw BltException(ErrorCode.VALIDATION_FAILED, "유효하지 않은 타임존입니다: $zone", ex)
    }
}
