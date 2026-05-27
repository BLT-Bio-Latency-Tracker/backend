package com.medilux.blt.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(val status: HttpStatus, val code: String, val message: String) {
    INVALID_REQUEST(HttpStatus.BAD_REQUEST, "INVALID_REQUEST", "Invalid request."),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED", "Authentication is required."),
    FORBIDDEN(HttpStatus.FORBIDDEN, "FORBIDDEN", "Access is denied."),
    RESOURCE_NOT_FOUND(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND", "Requested resource was not found."),
    INTERNAL_SERVER_ERROR(
        HttpStatus.INTERNAL_SERVER_ERROR,
        "INTERNAL_SERVER_ERROR",
        "An unexpected server error occurred.",
    ),

    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "USER_NOT_FOUND", "User was not found."),
    AUTH_INVALID_CREDENTIALS(HttpStatus.UNAUTHORIZED, "AUTH_INVALID_CREDENTIALS", "인증 자격 증명이 유효하지 않습니다."),
    TERMS_REQUIRED_NOT_AGREED(HttpStatus.BAD_REQUEST, "TERMS_REQUIRED_NOT_AGREED", "필수 약관에 동의해야 합니다."),
    ONBOARDING_ALREADY_COMPLETED(HttpStatus.CONFLICT, "ONBOARDING_ALREADY_COMPLETED", "이미 온보딩이 완료된 사용자입니다."),
    PARTIAL_ONBOARDING_NOT_ALLOWED(
        HttpStatus.UNPROCESSABLE_ENTITY,
        "PARTIAL_ONBOARDING_NOT_ALLOWED",
        "온보딩 프로필은 전체 입력하거나 완전히 비워두어야 합니다.",
    ),
    VALIDATION_FAILED(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED", "입력값 검증에 실패했습니다."),
    SERVICE_UNAVAILABLE(HttpStatus.SERVICE_UNAVAILABLE, "SERVICE_UNAVAILABLE", "외부 인증 기관 통신에 실패했습니다."),

    INVALID_PVT(HttpStatus.BAD_REQUEST, "INVALID_PVT", "Invalid PVT result."),
    INVALID_SLEEP_DATA(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_DATA", "Invalid sleep data."),
    BRAIN_ROI_NOT_FOUND(HttpStatus.NOT_FOUND, "BRAIN_ROI_NOT_FOUND", "Brain ROI score was not found."),
}
