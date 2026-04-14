package com.carrefourbank.transaction.domain.exception;

import com.carrefourbank.common.exception.BusinessException;

public class PeriodClosedException extends BusinessException {

    public PeriodClosedException(String date) {
        super("Accounting period is closed for date: " + date);
    }
}
