package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.ConsentLog
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface ConsentLogRepository : JpaRepository<ConsentLog, Long> {
    /** 약관 동의 이력(append-only) 최신순 — 같은 항목이 버전별로 여러 row일 수 있음. */
    fun findByUserIdOrderByAgreedAtDescIdDesc(userId: Long): List<ConsentLog>

    @Modifying
    @Query(
        """
        UPDATE consent_logs
           SET client_ip = NULL
         WHERE user_id = :userId
        """,
        nativeQuery = true,
    )
    fun clearClientIpByUserId(@Param("userId") userId: Long): Int
}
