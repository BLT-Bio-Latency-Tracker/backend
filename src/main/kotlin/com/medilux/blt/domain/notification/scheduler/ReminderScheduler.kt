package com.medilux.blt.domain.notification.scheduler

import com.medilux.blt.domain.notification.service.ReminderDispatchService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

/**
 * 매 분 데일리 리마인더 디스패치를 트리거한다.
 *
 * cron 의 zone 은 트리거 시점 해석용일 뿐(매 분이라 무관), 실제 발송 시각 판정은
 * [ReminderDispatchService] 가 사용자별 타임존 기준으로 수행한다.
 * MVP 는 단일 인스턴스 가정.
 */
@Component
class ReminderScheduler(private val reminderDispatchService: ReminderDispatchService) {
    @Scheduled(cron = "0 * * * * *", zone = "Asia/Seoul")
    fun dispatchDueReminders() {
        reminderDispatchService.dispatchDueReminders()
    }
}
