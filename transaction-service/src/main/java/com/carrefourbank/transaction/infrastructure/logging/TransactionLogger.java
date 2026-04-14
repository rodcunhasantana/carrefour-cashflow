package com.carrefourbank.transaction.infrastructure.logging;

import com.carrefourbank.transaction.domain.model.Transaction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class TransactionLogger {

    private static final Logger log = LoggerFactory.getLogger(
            "com.carrefourbank.transaction.infrastructure.logging.TransactionLogger");

    public void logCreated(Transaction transaction) {
        MDC.put("transactionId", transaction.id().toString());
        try {
            log.info("Transaction created: id={} type={} amount={} date={}",
                    transaction.id(), transaction.type(), transaction.amount(), transaction.date());
        } finally {
            MDC.remove("transactionId");
        }
    }

    public void logReversed(Transaction original, Transaction reversal) {
        MDC.put("transactionId", original.id().toString());
        try {
            log.info("Transaction reversed: originalId={} reversalId={}",
                    original.id(), reversal.id());
        } finally {
            MDC.remove("transactionId");
        }
    }
}
