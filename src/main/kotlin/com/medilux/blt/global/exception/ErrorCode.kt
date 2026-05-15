package com.medilux.blt.global.exception

import org.springframework.http.HttpStatus

enum class ErrorCode(
    val status: HttpStatus,
    val code: String,
    val message: String,
) {
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
    INVALID_PVT(HttpStatus.BAD_REQUEST, "INVALID_PVT", "Invalid PVT result."),
    INVALID_SLEEP_DATA(HttpStatus.BAD_REQUEST, "INVALID_SLEEP_DATA", "Invalid sleep data."),
    BRAIN_ROI_NOT_FOUND(HttpStatus.NOT_FOUND, "BRAIN_ROI_NOT_FOUND", "Brain ROI score was not found."),
}
