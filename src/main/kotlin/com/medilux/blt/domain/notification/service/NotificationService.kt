package com.medilux.blt.domain.notification.service

import com.medilux.blt.domain.notification.dto.NotificationCategory
import com.medilux.blt.domain.notification.dto.NotificationPageResponse
import com.medilux.blt.domain.user.entity.NotificationStatus
import com.medilux.blt.domain.user.repository.NotificationLogRepository
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import org.springframework.data.domain.PageRequest
import org.springframework.stereotype.Service
import org.springframework.transaction.annotation.Transactional
import java.time.Instant

@Service
class NotificationService(private val notificationLogRepository: NotificationLogRepository) {
    @Transactional(readOnly = true)
    fun list(userId: Long, category: NotificationCategory, page: Int, size: Int): NotificationPageResponse {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw BltException(ErrorCode.VALIDATION_FAILED)
        }
        val pageable = PageRequest.of(page, size)
        val result = if (category.types == null) {
            notificationLogRepository.findByUserIdOrderByScheduledAtDesc(userId, pageable)
        } else {
            notificationLogRepository.findByUserIdAndNotificationTypeInOrderByScheduledAtDesc(
                userId,
                category.types,
                pageable,
            )
        }
        return NotificationPageResponse.from(result)
    }

    @Transactional
    fun markRead(userId: Long, logId: Long) {
        val log = notificationLogRepository.findByIdAndUserId(logId, userId)
            ?: throw BltException(ErrorCode.NOTIFICATION_NOT_FOUND)
        if (log.openedAt == null) {
            log.openedAt = Instant.now()
            log.status = NotificationStatus.OPENED
        }
    }

    @Transactional
    fun markAllRead(userId: Long) {
        notificationLogRepository.markAllReadByUserId(userId, Instant.now())
    }

    @Transactional
    fun deleteAll(userId: Long) {
        notificationLogRepository.softDeleteAllByUserId(userId, Instant.now())
    }

    private companion object {
        const val MAX_PAGE_SIZE = 100
    }
}
