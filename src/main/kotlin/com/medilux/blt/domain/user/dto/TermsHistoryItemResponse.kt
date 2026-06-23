package com.medilux.blt.domain.user.dto

import com.medilux.blt.domain.user.entity.ConsentLog
import com.medilux.blt.domain.user.entity.ConsentType
import io.swagger.v3.oas.annotations.media.Schema
import java.time.Instant

@Schema(description = "약관 동의 이력 항목 (append-only, 최신순)")
data class TermsHistoryItemResponse(
    @field:Schema(description = "동의 항목", example = "TERMS_OF_SERVICE")
    val consentType: ConsentType,
    @field:Schema(description = "동의한 약관 버전", example = "1.0")
    val policyVersion: String,
    @field:Schema(description = "동의 여부")
    val agreed: Boolean,
    @field:Schema(description = "동의 시각")
    val agreedAt: Instant,
) {
    companion object {
        fun from(log: ConsentLog): TermsHistoryItemResponse = TermsHistoryItemResponse(
            consentType = log.consentType,
            policyVersion = log.policyVersion,
            agreed = log.agreed,
            agreedAt = log.agreedAt,
        )
    }
}
