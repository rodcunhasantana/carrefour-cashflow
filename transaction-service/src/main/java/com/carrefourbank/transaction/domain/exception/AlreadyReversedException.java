package com.carrefourbank.transaction.domain.exception;

import com.carrefourbank.common.exception.ValidationException;

public class AlreadyReversedException extends ValidationException {

    public AlreadyReversedException(String transactionId) {
        super("Transaction already reversed: " + transactionId);
    }
}
