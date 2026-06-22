package com.medilux.blt.domain.roi.service

import org.springframework.stereotype.Component

/**
 * Score_PVT 산출기
 */
@Component
class PvtScoreCalculator {
    /** Score_PVT ∈ [0, 100] **/
    fun calculate(avgRtMs: Double): Double = when {
        avgRtMs <= LOWER_BOUND_MS -> 100.0

        avgRtMs >= UPPER_BOUND_MS -> 0.0

        else -> {
            val ratio = (avgRtMs - LOWER_BOUND_MS) / SPAN_MS
            100.0 * (1 - ratio * ratio)
        }
    }

    companion object {
        const val LOWER_BOUND_MS = 250.0
        const val UPPER_BOUND_MS = 500.0
        const val SPAN_MS = UPPER_BOUND_MS - LOWER_BOUND_MS
    }
}
