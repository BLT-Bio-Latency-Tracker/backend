package com.medilux.blt.domain.notification.dto

import com.medilux.blt.domain.user.entity.NotificationLog
import com.medilux.blt.domain.user.entity.NotificationStatus
import com.medilux.blt.domain.user.entity.NotificationType
import io.swagger.v3.oas.annotations.media.Schema
import org.springframework.data.domain.Page
import java.time.Instant

/** 알림함 필터 탭(전체/측정/리포트) → NotificationType 매핑. */
enum class NotificationCategory(val types: Set<NotificationType>?) {
    ALL(null),
    MEASUREMENT(setOf(NotificationType.PVT_REMINDER, NotificationType.SLEEP_REMINDER)),
    REPORT(setOf(NotificationType.ROI_SCORE, NotificationType.RECOMMENDATION)),
}

@Schema(description = "알림함 항목")
data class NotificationItemResponse(
    val logId: Long,
    val notificationType: NotificationType,
    val title: String,
    val body: String,
    val status: NotificationStatus,
    @field:Schema(description = "읽음 여부 (openedAt != null)")
    val isRead: Boolean,
    val scheduledAt: Instant,
    val sentAt: Instant?,
    val openedAt: Instant?,
    val payload: Map<String, Any?>,
) {
    companion object {
        fun from(log: NotificationLog): NotificationItemResponse = NotificationItemResponse(
            logId = log.id,
            notificationType = log.notificationType,
            title = log.title,
            body = log.body,
            status = log.status,
            isRead = log.openedAt != null,
            scheduledAt = log.scheduledAt,
            sentAt = log.sentAt,
            openedAt = log.openedAt,
            payload = log.payload,
        )
    }
}

@Schema(description = "알림함 페이지 응답")
data class NotificationPageResponse(val items: List<NotificationItemResponse>, val page: Int, val size: Int, val hasNext: Boolean) {
    companion object {
        fun from(page: Page<NotificationLog>): NotificationPageResponse = NotificationPageResponse(
            items = page.content.map(NotificationItemResponse::from),
            page = page.number,
            size = page.size,
            hasNext = page.hasNext(),
        )
    }
}
