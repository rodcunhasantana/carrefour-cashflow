package com.carrefourbank.dailybalance.infrastructure.web;

import com.carrefourbank.common.exception.NotFoundException;
import com.carrefourbank.common.exception.ValidationException;
import com.carrefourbank.dailybalance.domain.exception.BalanceAlreadyClosedException;
import com.carrefourbank.dailybalance.domain.exception.BalanceAlreadyOpenException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(NotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNotFound(NotFoundException ex) {
        return error("RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(BalanceAlreadyClosedException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleAlreadyClosed(BalanceAlreadyClosedException ex) {
        return error("BALANCE_ALREADY_CLOSED", ex.getMessage());
    }

    @ExceptionHandler(BalanceAlreadyOpenException.class)
    @ResponseStatus(HttpStatus.CONFLICT)
    public ErrorResponse handleAlreadyOpen(BalanceAlreadyOpenException ex) {
        return error("BALANCE_ALREADY_OPEN", ex.getMessage());
    }

    @ExceptionHandler(ValidationException.class)
    @ResponseStatus(HttpStatus.UNPROCESSABLE_ENTITY)
    public ErrorResponse handleValidation(ValidationException ex) {
        return error("VALIDATION_ERROR", ex.getMessage());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ErrorResponse handleMethodArgumentNotValid(MethodArgumentNotValidException ex) {
        String message = ex.getBindingResult().getFieldErrors().stream()
                .map(fe -> fe.getField() + ": " + fe.getDefaultMessage())
                .findFirst()
                .orElse(ex.getMessage());
        return error("VALIDATION_ERROR", message);
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse handleNoResourceFound(NoResourceFoundException ex) {
        return error("RESOURCE_NOT_FOUND", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ErrorResponse handleGeneral(Exception ex) {
        return error("INTERNAL_ERROR", "An unexpected error occurred");
    }

    private ErrorResponse error(String code, String message) {
        return new ErrorResponse(code, message, Instant.now().toString());
    }
}
