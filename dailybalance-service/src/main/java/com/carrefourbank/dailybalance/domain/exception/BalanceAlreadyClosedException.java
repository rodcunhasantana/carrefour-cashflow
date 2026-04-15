package com.carrefourbank.dailybalance.domain.exception;

import com.carrefourbank.common.exception.ValidationException;

import java.time.LocalDate;

public class BalanceAlreadyClosedException extends ValidationException {
    public BalanceAlreadyClosedException(LocalDate date) {
        super("Daily balance for date " + date + " is already closed");
    }
}
