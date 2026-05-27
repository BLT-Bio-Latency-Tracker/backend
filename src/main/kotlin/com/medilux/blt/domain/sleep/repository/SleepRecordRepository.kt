package com.medilux.blt.domain.sleep.repository

import com.medilux.blt.domain.sleep.entity.SleepRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface SleepRecordRepository : JpaRepository<SleepRecord, Long> {
    @Modifying
    @Query(
        """
        UPDATE sleep_records
           SET raw_payload = NULL
         WHERE user_id = :userId
        """,
        nativeQuery = true,
    )
    fun clearRawPayloadByUserId(@Param("userId") userId: Long): Int
}
