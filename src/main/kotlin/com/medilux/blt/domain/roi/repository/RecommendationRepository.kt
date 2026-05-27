package com.medilux.blt.domain.roi.repository

import com.medilux.blt.domain.roi.entity.Recommendation
import org.springframework.data.jpa.repository.JpaRepository
import org.springframework.data.jpa.repository.Modifying
import org.springframework.data.jpa.repository.Query
import org.springframework.data.repository.query.Param

interface RecommendationRepository : JpaRepository<Recommendation, Long> {
    @Modifying
    @Query(
        """
        DELETE FROM Recommendation r
         WHERE r.roiScore.user.id = :userId
        """,
    )
    fun deleteAllByUserId(@Param("userId") userId: Long): Int
}
