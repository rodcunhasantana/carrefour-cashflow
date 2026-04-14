package com.carrefourbank.transaction.infrastructure.web;

import com.carrefourbank.common.exception.NotFoundException;
import com.carrefourbank.common.exception.ValidationException;
import com.carrefourbank.transaction.domain.exception.AlreadyReversedException;
import com.carrefourbank.transaction.domain.exception.PeriodClosedException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<ErrorResponse> handleNotFound(NotFoundException ex) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(error("RESOURCE_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AlreadyReversedException.class)
    public ResponseEntity<ErrorResponse> handleAlreadyReversed(AlreadyReversedException ex) {
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(error("ALREADY_REVERSED", ex.getMessage()));
    }

    @ExceptionHandler(PeriodClosedException.class)
    public ResponseEntity<ErrorResponse> handlePeriodClosed(PeriodClosedException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error("PERIOD_CLOSED", ex.getMessage()));
    }

    @ExceptionHandler(ValidationException.class)
    public ResponseEntity<ErrorResponse> handleValidation(ValidationException ex) {
        return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                .body(error("VALIDATION_ERROR", ex.getMessage()));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse("Validation failed");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(error("VALIDATION_ERROR", message));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGeneric(Exception ex) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(error("INTERNAL_ERROR", "An unexpected error occurred"));
    }

    private ErrorResponse error(String code, String message) {
        return new ErrorResponse(code, message, Instant.now().toString());
    }
}
