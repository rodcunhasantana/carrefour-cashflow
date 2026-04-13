// ValidationException.java
package com.carrefourbank.common.exception;

/**
 * Exceção lançada quando uma validação de regra de negócio falha.
 */
public class ValidationException extends BusinessException {
    public ValidationException(String message) {
        super(message);
    }
}