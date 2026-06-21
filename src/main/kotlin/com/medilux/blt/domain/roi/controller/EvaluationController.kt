package com.medilux.blt.domain.roi.controller

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.roi.dto.EvaluationCreateRequest
import com.medilux.blt.domain.roi.dto.EvaluationResponse
import com.medilux.blt.domain.roi.service.EvaluationService
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import org.springframework.http.HttpStatus
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/api/v1/evaluations")
@Tag(name = "Evaluations", description = "Brain ROI 평가 제출·조회 API")
class EvaluationController(private val evaluationService: EvaluationService) {
    @Operation(summary = "ROI 평가 제출 (점수 계산 + 추천 반환)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun submit(@AuthenticationPrincipal principal: AuthUserPrincipal?, @RequestBody request: EvaluationCreateRequest): EvaluationResponse =
        evaluationService.submit(requireUserId(principal), request)

    private fun requireUserId(principal: AuthUserPrincipal?): Long =
        principal?.userId ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
}
