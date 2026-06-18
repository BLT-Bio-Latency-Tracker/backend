package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.UserDevice
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface UserDeviceRepository : JpaRepository<UserDevice, Long> {
    fun findByUserIdAndFcmToken(userId: Long, fcmToken: String): UserDevice?

    fun findByIdAndUserId(id: Long, userId: Long): UserDevice?

    fun findByUserIdAndRevokedAtIsNull(userId: Long): List<UserDevice>

    /** 같은 FCM 토큰이 다른 사용자에 활성화돼 있으면 회수(한 디바이스 = 한 사용자). */
    @Modifying
    @Query(
        """
        UPDATE UserDevice d
           SET d.revokedAt = :now
         WHERE d.fcmToken = :fcmToken
           AND d.user.id <> :userId
           AND d.revokedAt IS NULL
        """,
    )
    fun revokeActiveByFcmTokenForOtherUsers(
        @Param("fcmToken") fcmToken: String,
        @Param("userId") userId: Long,
        @Param("now") now: Instant,
    ): Int

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
