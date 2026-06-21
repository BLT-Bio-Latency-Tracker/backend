package com.medilux.blt.domain.pvt.repository

import com.medilux.blt.domain.pvt.entity.PvtSession
import org.springframework.data.jpa.repository.JpaRepository

interface PvtSessionRepository : JpaRepository<PvtSession, Long>
