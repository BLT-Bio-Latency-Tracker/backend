package com.medilux.blt.global.exception

import jakarta.validation.ConstraintViolationException
import org.slf4j.LoggerFactory
import org.springframework.http.HttpStatus
import org.springframework.http.ProblemDetail
import org.springframework.http.ResponseEntity
import org.springframework.http.converter.HttpMessageNotReadableException
import org.springframework.security.access.AccessDeniedException
import org.springframework.security.core.AuthenticationException
import org.springframework.validation.FieldError
import org.springframework.web.bind.MethodArgumentNotValidException
import org.springframework.web.bind.annotation.ExceptionHandler
import org.springframework.web.bind.annotation.RestControllerAdvice
import org.springframework.web.servlet.resource.NoResourceFoundException

@RestControllerAdvice
class GlobalExceptionHandler {
    private val log = LoggerFactory.getLogger(javaClass)

    @ExceptionHandler(BltException::class)
    fun handleBltException(ex: BltException): ResponseEntity<ProblemDetail> = buildProblemDetail(
        status = ex.errorCode.status,
        detail = ex.message,
        errorCode = ex.errorCode.code,
    )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(ex: MethodArgumentNotValidException): ResponseEntity<ProblemDetail> {
        val fieldErrors =
            ex.bindingResult
                .fieldErrors
                .map { fieldError -> fieldError.toErrorResponse() }

        val problemDetail =
            ProblemDetail.forStatusAndDetail(
                ErrorCode.VALIDATION_FAILED.status,
                ErrorCode.VALIDATION_FAILED.message,
            )
        problemDetail.title = ErrorCode.VALIDATION_FAILED.status.reasonPhrase
        problemDetail.setProperty("errorCode", ErrorCode.VALIDATION_FAILED.code)
        problemDetail.setProperty("fieldErrors", fieldErrors)

        return ResponseEntity.status(ErrorCode.VALIDATION_FAILED.status).body(problemDetail)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ProblemDetail> = buildProblemDetail(
        status = ErrorCode.VALIDATION_FAILED.status,
        detail = ex.message ?: ErrorCode.VALIDATION_FAILED.message,
        errorCode = ErrorCode.VALIDATION_FAILED.code,
    )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> = buildProblemDetail(
        status = ErrorCode.VALIDATION_FAILED.status,
        detail = ErrorCode.VALIDATION_FAILED.message,
        errorCode = ErrorCode.VALIDATION_FAILED.code,
    )

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ProblemDetail> = buildProblemDetail(
        status = ErrorCode.UNAUTHORIZED.status,
        detail = ex.message ?: ErrorCode.UNAUTHORIZED.message,
        errorCode = ErrorCode.UNAUTHORIZED.code,
    )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ProblemDetail> = buildProblemDetail(
        status = ErrorCode.FORBIDDEN.status,
        detail = ex.message ?: ErrorCode.FORBIDDEN.message,
        errorCode = ErrorCode.FORBIDDEN.code,
    )

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<ProblemDetail> = buildProblemDetail(
        status = ErrorCode.RESOURCE_NOT_FOUND.status,
        detail = ex.message ?: ErrorCode.RESOURCE_NOT_FOUND.message,
        errorCode = ErrorCode.RESOURCE_NOT_FOUND.code,
    )

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ProblemDetail> {
        // 예상 못 한 5xx만 ERROR 로깅 → 컨테이너 로그 기록 + Sentry(Logback 통합)로 전송.
        // (4xx 비즈니스 예외는 정상 응답이라 Sentry로 보내지 않음)
        log.error("Unhandled exception", ex)
        return buildProblemDetail(
            status = ErrorCode.INTERNAL_SERVER_ERROR.status,
            detail = ErrorCode.INTERNAL_SERVER_ERROR.message,
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR.code,
        )
    }

    private fun buildProblemDetail(status: HttpStatus, detail: String, errorCode: String): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
        problemDetail.title = status.reasonPhrase
        problemDetail.setProperty("errorCode", errorCode)

        return ResponseEntity.status(status).body(problemDetail)
    }

    private fun FieldError.toErrorResponse(): ValidationErrorResponse = ValidationErrorResponse(
        field = field,
        message = defaultMessage ?: ErrorCode.VALIDATION_FAILED.message,
    )

    data class ValidationErrorResponse(val field: String, val message: String)
}
