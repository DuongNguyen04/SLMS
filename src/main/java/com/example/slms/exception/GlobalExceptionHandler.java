package com.example.slms.exception;

import java.time.Instant;
import java.util.List;

import org.springframework.http.HttpStatusCode;
import org.springframework.http.HttpStatus;
import org.springframework.security.authorization.AuthorizationDeniedException;
import org.springframework.http.ResponseEntity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import com.example.slms.dto.response.ApiErrorResponse;

import jakarta.validation.ConstraintViolationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger LOGGER = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiErrorResponse> handleValidation(MethodArgumentNotValidException ex) {
        List<String> details = ex.getBindingResult().getFieldErrors().stream()
                .map(this::formatFieldError)
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", details);
    }

    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<ApiErrorResponse> handleConstraintViolation(ConstraintViolationException ex) {
        List<String> details = ex.getConstraintViolations().stream()
                .map(violation -> violation.getPropertyPath() + ": " + violation.getMessage())
                .toList();

        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", details);
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ApiErrorResponse> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        String detail = ex.getName() + ": invalid value '" + ex.getValue() + "'";
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", "Request validation failed", List.of(detail));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ApiErrorResponse> handleValidationException(ValidationException ex) {
        return buildResponse(HttpStatus.BAD_REQUEST, "VALIDATION_ERROR", ex.getMessage(), List.of());
    }

    @ExceptionHandler(AuthorizationException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorization(AuthorizationException ex) {
        return buildResponse(HttpStatus.UNAUTHORIZED, "AUTHORIZATION_ERROR", ex.getMessage(), List.of());
    }

    @ExceptionHandler(AuthorizationDeniedException.class)
    public ResponseEntity<ApiErrorResponse> handleAuthorizationDenied(AuthorizationDeniedException ex) {
        return buildResponse(HttpStatus.FORBIDDEN, "AUTHORIZATION_ERROR", "Access denied", List.of());
    }

    @ExceptionHandler(ConcurrencyException.class)
    public ResponseEntity<ApiErrorResponse> handleConcurrency(ConcurrencyException ex) {
        return buildResponse(HttpStatus.CONFLICT, "CONCURRENCY_CONFLICT", ex.getMessage(), List.of());
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiErrorResponse> handleBusiness(BusinessException ex) {
        return buildResponse(ex.getStatus(), mapBusinessErrorCode(ex.getStatus()), ex.getMessage(), List.of());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiErrorResponse> handleGeneric(Exception ex) {
        LOGGER.error("Unhandled exception", ex);
        return buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL_SERVER_ERROR", "Unexpected error", List.of());
    }

    private String formatFieldError(FieldError fieldError) {
        return fieldError.getField() + ": " + fieldError.getDefaultMessage();
    }

    private ResponseEntity<ApiErrorResponse> buildResponse(
            HttpStatusCode status,
            String error,
            String message,
            List<String> details) {
        ApiErrorResponse response = ApiErrorResponse.builder()
                .error(error)
                .message(message)
                .details(details)
                .timestamp(Instant.now())
                .build();
        return ResponseEntity.status(status).body(response);
    }

    private String mapBusinessErrorCode(HttpStatusCode status) {
        if (status.value() == HttpStatus.BAD_REQUEST.value()) {
            return "VALIDATION_ERROR";
        }

        if (status.value() == HttpStatus.UNAUTHORIZED.value()
                || status.value() == HttpStatus.FORBIDDEN.value()) {
            return "AUTHORIZATION_ERROR";
        }

        if (status.value() == HttpStatus.CONFLICT.value()) {
            return "CONCURRENCY_CONFLICT";
        }

        return "BUSINESS_ERROR";
    }
}
