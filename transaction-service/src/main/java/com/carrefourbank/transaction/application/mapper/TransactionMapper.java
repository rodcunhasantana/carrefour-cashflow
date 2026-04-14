package com.carrefourbank.transaction.application.mapper;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.transaction.application.command.TransactionCreateCommand;
import com.carrefourbank.transaction.application.dto.CreateTransactionRequest;
import com.carrefourbank.transaction.application.dto.TransactionDTO;
import com.carrefourbank.transaction.domain.model.Transaction;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class TransactionMapper {

    public TransactionDTO toDTO(Transaction transaction) {
        String originalId = transaction.originalTransactionId() != null
                ? transaction.originalTransactionId().toString()
                : null;
        return new TransactionDTO(
                transaction.id().toString(),
                transaction.type(),
                transaction.amount().amount().setScale(2, RoundingMode.HALF_UP).toPlainString(),
                transaction.amount().currency().name(),
                transaction.date(),
                transaction.description(),
                transaction.createdAt(),
                transaction.status(),
                transaction.isReversal(),
                originalId);
    }

    public TransactionCreateCommand toCommand(CreateTransactionRequest request) {
        return new TransactionCreateCommand(
                request.type(),
                request.amount(),
                request.date(),
                request.description());
    }

    public Money toMoney(BigDecimal amount, String currencyCode) {
        return Money.of(amount, Currency.valueOf(currencyCode));
    }
}
