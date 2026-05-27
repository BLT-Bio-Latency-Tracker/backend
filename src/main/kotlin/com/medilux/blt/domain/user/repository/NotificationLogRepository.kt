package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.NotificationLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface NotificationLogRepository : JpaRepository<NotificationLog, Long> {
    @Modifying
    @Query(
        """
        DELETE FROM NotificationLog n
         WHERE n.user.id = :userId
        """,
    )
    fun deleteAllByUserId(@Param("userId") userId: Long): Int
}
