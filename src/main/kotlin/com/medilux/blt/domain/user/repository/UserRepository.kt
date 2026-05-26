package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.User
import org.springframework.data.jpa.repository.JpaRepository

interface UserRepository : JpaRepository<User, Long> {
    fun findByAppleSubHash(appleSubHash: String): User?

    fun existsByAppleSubHash(appleSubHash: String): Boolean
}
