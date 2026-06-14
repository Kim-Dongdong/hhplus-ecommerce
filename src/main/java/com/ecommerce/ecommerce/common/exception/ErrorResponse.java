package com.ecommerce.ecommerce.common.exception;

import java.time.LocalDateTime;
import java.util.List;
import org.springframework.http.HttpStatus;

public record ErrorResponse(
        LocalDateTime timestamp,
        int status,
        String code,
        String message,
        List<FieldError> fieldErrors
) {

    public record FieldError(String field, String reason) {
    }

    public static ErrorResponse of(ErrorCode errorCode) {
        return of(errorCode, errorCode.getMessage());
    }

    public static ErrorResponse of(ErrorCode errorCode, String message) {
        return new ErrorResponse(LocalDateTime.now(), errorCode.getStatus().value(), errorCode.name(), message, List.of());
    }

    public static ErrorResponse of(HttpStatus status, String code, String message) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), code, message, List.of());
    }

    public static ErrorResponse of(HttpStatus status, String code, String message, List<FieldError> fieldErrors) {
        return new ErrorResponse(LocalDateTime.now(), status.value(), code, message, fieldErrors);
    }
}
