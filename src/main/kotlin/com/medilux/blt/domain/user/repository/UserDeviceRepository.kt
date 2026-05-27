package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.UserDevice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface UserDeviceRepository : JpaRepository<UserDevice, Long> {
    @Modifying
    @Query(
        """
        UPDATE UserDevice d
           SET d.revokedAt = :now
         WHERE d.user.id = :userId
           AND d.revokedAt IS NULL
        """,
    )
    fun revokeAllActiveByUserId(@Param("userId") userId: Long, @Param("now") now: Instant): Int

    @Modifying
    @Query(
        """
        DELETE FROM UserDevice d
         WHERE d.user.id = :userId
        """,
    )
    fun deleteAllByUserId(@Param("userId") userId: Long): Int
}
