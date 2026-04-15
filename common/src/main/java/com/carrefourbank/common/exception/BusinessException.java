package com.carrefourbank.common.exception;

/**
 * Exceção base para erros de negócio.
 * Todas as exceções de regras de negócio devem estender esta classe.
 */
public abstract class BusinessException extends RuntimeException {
    public BusinessException(String message) {
        super(message);
    }
    
    public BusinessException(String message, Throwable cause) {
        super(message, cause);
    }
}