package com.carrefourbank.transaction.infrastructure.adapter.pubsub;

import com.carrefourbank.transaction.domain.model.Transaction;
import com.carrefourbank.transaction.domain.port.TransactionEventPublisher;
import com.carrefourbank.transaction.infrastructure.adapter.pubsub.event.TransactionCreatedEventData;
import com.carrefourbank.transaction.infrastructure.adapter.pubsub.event.TransactionEventEnvelope;
import com.carrefourbank.transaction.infrastructure.adapter.pubsub.event.TransactionReversedEventData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cloud.gcp.pubsub.core.PubSubTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.UUID;

@Component
@Profile("!test")
public class PubSubTransactionEventPublisher implements TransactionEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PubSubTransactionEventPublisher.class);
    private static final String TOPIC = "transaction-events";
    private static final String PRODUCER = "transaction-service";

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    public PubSubTransactionEventPublisher(PubSubTemplate pubSubTemplate, ObjectMapper objectMapper) {
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = "default")
    @CircuitBreaker(name = "default")
    public void publishTransactionCreatedEvent(Transaction transaction) {
        TransactionCreatedEventData data = new TransactionCreatedEventData(
                transaction.id().toString(),
                transaction.type().name(),
                transaction.amount().amount(),
                transaction.amount().currency().name(),
                transaction.date(),
                transaction.description());
        publish("transaction-created", data);
    }

    @Override
    @Retry(name = "default")
    @CircuitBreaker(name = "default")
    public void publishTransactionReversedEvent(Transaction original, Transaction reversal) {
        String reason = extractReason(reversal.description());
        TransactionReversedEventData data = new TransactionReversedEventData(
                original.id().toString(),
                reversal.id().toString(),
                reason,
                reversal.date());
        publish("transaction-reversed", data);
    }

    private void publish(String eventType, Object data) {
        TransactionEventEnvelope envelope = new TransactionEventEnvelope(
                UUID.randomUUID().toString(),
                eventType,
                Instant.now().toString(),
                PRODUCER,
                data);
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            pubSubTemplate.publish(TOPIC, payload);
            log.debug("Published event: type={} topic={}", eventType, TOPIC);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize event: " + eventType, e);
        }
    }

    private String extractReason(String description) {
        // Description format: "Reversal: <original> - Reason: <reason>"
        int reasonIndex = description.indexOf("- Reason: ");
        if (reasonIndex >= 0) {
            return description.substring(reasonIndex + "- Reason: ".length());
        }
        return description;
    }
}
