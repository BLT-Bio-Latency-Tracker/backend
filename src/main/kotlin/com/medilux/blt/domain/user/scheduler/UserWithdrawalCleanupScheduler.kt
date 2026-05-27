package com.medilux.blt.domain.user.scheduler

import com.medilux.blt.domain.user.service.UserWithdrawalCleanupService
import org.springframework.scheduling.annotation.Scheduled
import org.springframework.stereotype.Component

@Component
class UserWithdrawalCleanupScheduler(private val userWithdrawalCleanupService: UserWithdrawalCleanupService) {
    @Scheduled(cron = "0 0 3 * * *", zone = "Asia/Seoul")
    fun cleanupExpiredWithdrawals() {
        userWithdrawalCleanupService.cleanupExpiredWithdrawals()
    }
}
