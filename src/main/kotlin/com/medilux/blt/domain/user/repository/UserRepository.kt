package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserStatus
import org.springframework.data.domain.Pageable
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant
import java.time.LocalTime

interface UserRepository : JpaRepository<User, Long> {
    fun findByAppleSubHash(appleSubHash: String): User?

    fun existsByAppleSubHash(appleSubHash: String): Boolean

    fun findTop100ByStatusAndWithdrawnAtLessThanEqualOrderByIdAsc(status: UserStatus, withdrawnAt: Instant): List<User>

    @Query("SELECT DISTINCT u.notificationTimezone FROM User u WHERE u.notificationEnabled = true")
    fun findDistinctNotificationTimezones(): List<String>

    @Query(
        """
        SELECT u FROM User u
         WHERE u.notificationEnabled = true
           AND u.notificationTimezone = :zone
           AND u.pvtReminderTime = :localTime
           AND EXISTS (SELECT 1 FROM UserDevice d WHERE d.user = u AND d.revokedAt IS NULL)
        """,
    )
    fun findActiveUsersForPvtReminder(
        @Param("zone") zone: String,
        @Param("localTime") localTime: LocalTime,
        pageable: Pageable,
    ): List<User>

    @Query(
        """
        SELECT u FROM User u
         WHERE u.notificationEnabled = true
           AND u.notificationTimezone = :zone
           AND u.sleepReminderTime = :localTime
           AND EXISTS (SELECT 1 FROM UserDevice d WHERE d.user = u AND d.revokedAt IS NULL)
        """,
    )
    fun findActiveUsersForSleepReminder(
        @Param("zone") zone: String,
        @Param("localTime") localTime: LocalTime,
        pageable: Pageable,
    ): List<User>
}
