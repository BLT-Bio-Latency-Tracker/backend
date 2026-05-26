package com.medilux.blt.domain.user.repository

import com.medilux.blt.domain.user.entity.ConsentLog
import org.springframework.data.jpa.repository.JpaRepository

interface ConsentLogRepository : JpaRepository<ConsentLog, Long>
