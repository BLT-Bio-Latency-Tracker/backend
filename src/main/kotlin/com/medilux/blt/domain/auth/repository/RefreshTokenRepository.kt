package com.medilux.blt.domain.auth.repository

import com.medilux.blt.domain.auth.entity.RefreshToken
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.Instant

interface RefreshTokenRepository : JpaRepository<RefreshToken, Long> {
    fun findByTokenHash(tokenHash: String): RefreshToken?

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query(
        """
        update RefreshToken rt
           set rt.revokedAt = :revokedAt
         where rt.tokenHash = :tokenHash
           and rt.revokedAt is null
           and rt.expiresAt > :revokedAt
        """,
    )
    fun revokeActiveToken(@Param("tokenHash") tokenHash: String, @Param("revokedAt") revokedAt: Instant): Int

    @Modifying
    @Query(
        """
        UPDATE RefreshToken r
           SET r.revokedAt = :now
         WHERE r.user.id = :userId
           AND r.revokedAt IS NULL
           AND r.expiresAt > :now
        """,
    )
    fun revokeAllActiveByUserId(@Param("userId") userId: Long, @Param("now") now: Instant): Int

    @Modifying
    @Query(
        """
        DELETE FROM RefreshToken r
         WHERE r.user.id = :userId
        """,
    )
    fun deleteAllByUserId(@Param("userId") userId: Long): Int
}
