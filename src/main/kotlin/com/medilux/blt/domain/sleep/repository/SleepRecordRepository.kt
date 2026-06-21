package com.medilux.blt.domain.sleep.repository

import com.medilux.blt.domain.sleep.entity.SleepRecord
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param
import java.time.LocalDate

interface SleepRecordRepository : JpaRepository<SleepRecord, Long> {
    /** 수면 일자 공유: 그날 SleepRecord가 있으면 재사용/갱신. 같은 날 복수 시 최신 1건. */
    fun findFirstByUserIdAndSleepDateOrderByIdDesc(userId: Long, sleepDate: LocalDate): SleepRecord?

    /** 최근 7일 개인 baseline 계산용: 유효 야간(totalMinutes > 기준)만. */
    fun findByUserIdAndSleepDateBetweenAndTotalMinutesGreaterThan(
        userId: Long,
        start: LocalDate,
        end: LocalDate,
        totalMinutes: Int,
    ): List<SleepRecord>

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
