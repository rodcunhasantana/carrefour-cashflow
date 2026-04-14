package com.carrefourbank.transaction.domain.port;

import com.carrefourbank.transaction.domain.model.Transaction;

public interface TransactionEventPublisher {

    void publishTransactionCreatedEvent(Transaction transaction);

    void publishTransactionReversedEvent(Transaction original, Transaction reversal);
}
