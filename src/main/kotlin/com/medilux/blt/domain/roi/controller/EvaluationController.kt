package com.medilux.blt.domain.roi.controller

import com.medilux.blt.domain.auth.security.AuthUserPrincipal
import com.medilux.blt.domain.roi.dto.EvaluationCreateRequest
import com.medilux.blt.domain.roi.dto.EvaluationDetailResponse
import com.medilux.blt.domain.roi.dto.EvaluationPageResponse
import com.medilux.blt.domain.roi.dto.EvaluationResponse
import com.medilux.blt.domain.roi.dto.EvaluationStatsResponse
import com.medilux.blt.domain.roi.service.EvaluationService
import com.medilux.blt.global.exception.BltException
import com.medilux.blt.global.exception.ErrorCode
import io.swagger.v3.oas.annotations.Operation
import io.swagger.v3.oas.annotations.tags.Tag
import jakarta.validation.Valid
import org.springframework.format.annotation.DateTimeFormat
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RequestParam
import org.springframework.web.bind.annotation.ResponseStatus
import org.springframework.web.bind.annotation.RestController
import java.time.LocalDate

@RestController
@RequestMapping("/api/v1/evaluations")
@Tag(name = "Evaluations", description = "Brain ROI 평가 제출·조회 API")
class EvaluationController(private val evaluationService: EvaluationService) {
    @Operation(summary = "ROI 평가 제출 (점수 계산 + 추천 반환)")
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    fun submit(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @Valid @RequestBody request: EvaluationCreateRequest,
    ): EvaluationResponse = evaluationService.submit(requireUserId(principal), request)

    @Operation(summary = "오늘의 평가 조회")
    @GetMapping("/today")
    fun getToday(@AuthenticationPrincipal principal: AuthUserPrincipal?): ResponseEntity<EvaluationResponse> =
        evaluationService.getToday(requireUserId(principal))
            ?.let { ResponseEntity.ok(it) }
            ?: ResponseEntity.noContent().build()

    @Operation(summary = "평가 통계 (측정 일수/평균/최고/최저)")
    @GetMapping("/stats")
    fun stats(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @RequestParam(defaultValue = "month") period: String,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
    ): EvaluationStatsResponse = evaluationService.stats(requireUserId(principal), period, from, to)

    @Operation(summary = "평가 목록 조회")
    @GetMapping
    fun list(
        @AuthenticationPrincipal principal: AuthUserPrincipal?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) from: LocalDate?,
        @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) to: LocalDate?,
        @RequestParam(required = false) cursor: String?,
        @RequestParam(defaultValue = "31") size: Int,
    ): EvaluationPageResponse = evaluationService.list(requireUserId(principal), from, to, cursor, size)

    @Operation(summary = "평가 상세 조회 (수면, PVT 카드)")
    @GetMapping("/{id}")
    fun getDetail(@AuthenticationPrincipal principal: AuthUserPrincipal?, @PathVariable id: Long): EvaluationDetailResponse =
        evaluationService.getDetail(requireUserId(principal), id)

    private fun requireUserId(principal: AuthUserPrincipal?): Long =
        principal?.userId ?: throw BltException(ErrorCode.AUTH_INVALID_CREDENTIALS)
}
