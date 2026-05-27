package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.User
import com.medilux.blt.domain.user.entity.UserStatus
import org.springframework.data.jpa.repository.JpaRepository
import java.time.Instant

interface UserRepository : JpaRepository<User, Long> {
    fun findByAppleSubHash(appleSubHash: String): User?

    fun existsByAppleSubHash(appleSubHash: String): Boolean

    fun findTop100ByStatusAndWithdrawnAtLessThanEqualOrderByIdAsc(status: UserStatus, withdrawnAt: Instant): List<User>
}
