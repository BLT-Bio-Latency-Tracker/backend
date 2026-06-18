package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.NotificationLog
import com.medilux.blt.domain.user.entity.NotificationType
import org.springframework.data.domain.Page
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface NotificationLogRepository : JpaRepository<NotificationLog, Long> {
    fun findByUserIdOrderByScheduledAtDesc(userId: Long, pageable: Pageable): Page<NotificationLog>

    fun findByUserIdAndNotificationTypeInOrderByScheduledAtDesc(
        userId: Long,
        notificationTypes: Collection<NotificationType>,
        pageable: Pageable,
    ): Page<NotificationLog>

    fun findByIdAndUserId(id: Long, userId: Long): NotificationLog?

    /** 리마인더 중복 방지: 해당 시점 이후 동일 (user, type) 알림이 이미 있는지. */
    fun existsByUserIdAndNotificationTypeAndScheduledAtGreaterThanEqual(
        userId: Long,
        notificationType: NotificationType,
        scheduledAt: Instant,
    ): Boolean

    @Modifying
    @Query(
        """
        UPDATE NotificationLog n
           SET n.status = com.medilux.blt.domain.user.entity.NotificationStatus.OPENED,
               n.openedAt = :now
         WHERE n.user.id = :userId
           AND n.openedAt IS NULL
        """,
    )
    fun markAllReadByUserId(@Param("userId") userId: Long, @Param("now") now: Instant): Int

    /** 모두 지우기: 사용자 알림 전체 soft delete(`deletedAt` 마킹 → `@SQLRestriction`으로 알림함에서 숨김). */
    @Modifying
    @Query(
        """
        UPDATE NotificationLog n
           SET n.deletedAt = :now
         WHERE n.user.id = :userId
           AND n.deletedAt IS NULL
        """,
    )
    fun softDeleteAllByUserId(@Param("userId") userId: Long, @Param("now") now: Instant): Int

    @Modifying
    @Query(
        """
        DELETE FROM NotificationLog n
         WHERE n.user.id = :userId
        """,
    )
    fun deleteAllByUserId(@Param("userId") userId: Long): Int
}
