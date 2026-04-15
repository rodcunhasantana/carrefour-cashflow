package com.carrefourbank.common.exception;

/**
 * Exceção lançada quando um recurso não é encontrado.
 */
public class NotFoundException extends BusinessException {
    public NotFoundException(String message) {
        super(message);
    }
}