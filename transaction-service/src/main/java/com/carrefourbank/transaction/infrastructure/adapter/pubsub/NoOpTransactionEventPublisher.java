package com.carrefourbank.transaction.infrastructure.adapter.pubsub;

import com.carrefourbank.transaction.domain.model.Transaction;
import com.carrefourbank.transaction.domain.port.TransactionEventPublisher;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class NoOpTransactionEventPublisher implements TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(NoOpTransactionEventPublisher.class);

    @Override
    public void publishTransactionCreatedEvent(Transaction transaction) {
        log.debug("NoOp: publishTransactionCreatedEvent for {}", transaction.id());
    }

    @Override
    public void publishTransactionReversedEvent(Transaction original, Transaction reversal) {
        log.debug("NoOp: publishTransactionReversedEvent original={} reversal={}", original.id(), reversal.id());
    }
}
