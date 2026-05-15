package com.medilux.blt.global.exception

import jakarta.validation.ConstraintViolationException
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

    @ExceptionHandler(BltException::class)
    fun handleBltException(ex: BltException): ResponseEntity<ProblemDetail> =
        buildProblemDetail(
            status = ex.errorCode.status,
            detail = ex.message,
            errorCode = ex.errorCode.code,
        )

    @ExceptionHandler(MethodArgumentNotValidException::class)
    fun handleMethodArgumentNotValidException(
        ex: MethodArgumentNotValidException,
    ): ResponseEntity<ProblemDetail> {
        val fieldErrors = ex.bindingResult
            .fieldErrors
            .map { fieldError -> fieldError.toErrorResponse() }

        val problemDetail = ProblemDetail.forStatusAndDetail(
            ErrorCode.INVALID_REQUEST.status,
            ErrorCode.INVALID_REQUEST.message,
        )
        problemDetail.title = ErrorCode.INVALID_REQUEST.status.reasonPhrase
        problemDetail.setProperty("errorCode", ErrorCode.INVALID_REQUEST.code)
        problemDetail.setProperty("fieldErrors", fieldErrors)

        return ResponseEntity.status(ErrorCode.INVALID_REQUEST.status).body(problemDetail)
    }

    @ExceptionHandler(ConstraintViolationException::class)
    fun handleConstraintViolationException(ex: ConstraintViolationException): ResponseEntity<ProblemDetail> =
        buildProblemDetail(
            status = ErrorCode.INVALID_REQUEST.status,
            detail = ex.message ?: ErrorCode.INVALID_REQUEST.message,
            errorCode = ErrorCode.INVALID_REQUEST.code,
        )

    @ExceptionHandler(HttpMessageNotReadableException::class)
    fun handleHttpMessageNotReadableException(ex: HttpMessageNotReadableException): ResponseEntity<ProblemDetail> =
        buildProblemDetail(
            status = ErrorCode.INVALID_REQUEST.status,
            detail = ex.message ?: ErrorCode.INVALID_REQUEST.message,
            errorCode = ErrorCode.INVALID_REQUEST.code,
        )

    @ExceptionHandler(AuthenticationException::class)
    fun handleAuthenticationException(ex: AuthenticationException): ResponseEntity<ProblemDetail> =
        buildProblemDetail(
            status = ErrorCode.UNAUTHORIZED.status,
            detail = ex.message ?: ErrorCode.UNAUTHORIZED.message,
            errorCode = ErrorCode.UNAUTHORIZED.code,
        )

    @ExceptionHandler(AccessDeniedException::class)
    fun handleAccessDeniedException(ex: AccessDeniedException): ResponseEntity<ProblemDetail> =
        buildProblemDetail(
            status = ErrorCode.FORBIDDEN.status,
            detail = ex.message ?: ErrorCode.FORBIDDEN.message,
            errorCode = ErrorCode.FORBIDDEN.code,
        )

    @ExceptionHandler(NoResourceFoundException::class)
    fun handleNoResourceFoundException(ex: NoResourceFoundException): ResponseEntity<ProblemDetail> =
        buildProblemDetail(
            status = ErrorCode.RESOURCE_NOT_FOUND.status,
            detail = ex.message ?: ErrorCode.RESOURCE_NOT_FOUND.message,
            errorCode = ErrorCode.RESOURCE_NOT_FOUND.code,
        )

    @ExceptionHandler(Exception::class)
    fun handleException(ex: Exception): ResponseEntity<ProblemDetail> =
        buildProblemDetail(
            status = ErrorCode.INTERNAL_SERVER_ERROR.status,
            detail = ErrorCode.INTERNAL_SERVER_ERROR.message,
            errorCode = ErrorCode.INTERNAL_SERVER_ERROR.code,
        )

    private fun buildProblemDetail(
        status: HttpStatus,
        detail: String,
        errorCode: String,
    ): ResponseEntity<ProblemDetail> {
        val problemDetail = ProblemDetail.forStatusAndDetail(status, detail)
        problemDetail.title = status.reasonPhrase
        problemDetail.setProperty("errorCode", errorCode)

        return ResponseEntity.status(status).body(problemDetail)
    }

    private fun FieldError.toErrorResponse(): ValidationErrorResponse =
        ValidationErrorResponse(
            field = field,
            message = defaultMessage ?: ErrorCode.INVALID_REQUEST.message,
        )

    data class ValidationErrorResponse(
        val field: String,
        val message: String,
    )
}
