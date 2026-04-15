package com.carrefourbank.dailybalance.domain.exception;

import com.carrefourbank.common.exception.ValidationException;

import java.time.LocalDate;

public class BalanceAlreadyOpenException extends ValidationException {
    public BalanceAlreadyOpenException(LocalDate date) {
        super("Daily balance for date " + date + " is already open");
    }
}
