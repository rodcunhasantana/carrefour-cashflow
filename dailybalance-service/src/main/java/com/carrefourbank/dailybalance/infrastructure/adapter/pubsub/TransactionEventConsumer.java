package com.carrefourbank.dailybalance.infrastructure.adapter.pubsub;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.dailybalance.application.port.DailyBalanceService;
import com.carrefourbank.dailybalance.infrastructure.adapter.pubsub.event.TransactionCreatedEventData;
import com.carrefourbank.dailybalance.infrastructure.adapter.pubsub.event.TransactionEventEnvelope;
import com.carrefourbank.dailybalance.infrastructure.adapter.pubsub.event.TransactionReversedEventData;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;

@Component
@Profile("!test")
public class TransactionEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(TransactionEventConsumer.class);
    private static final String SUBSCRIPTION = "dailybalance-transaction-subscription";

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;
    private final DailyBalanceService dailyBalanceService;

    public TransactionEventConsumer(PubSubTemplate pubSubTemplate, ObjectMapper objectMapper, DailyBalanceService dailyBalanceService) {
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
        this.dailyBalanceService = dailyBalanceService;
    }

    @PostConstruct
    public void startSubscription() {
        pubSubTemplate.subscribe(SUBSCRIPTION, message -> {
            String payload = message.getPubsubMessage().getData().toStringUtf8();
            try {
                TransactionEventEnvelope envelope = objectMapper.readValue(payload, TransactionEventEnvelope.class);
                processEvent(envelope);
                message.ack();
                log.debug("Processed event: eventId={} type={}", envelope.eventId(), envelope.eventType());
            } catch (Exception e) {
                log.error("Failed to process event from subscription {}: {}", SUBSCRIPTION, e.getMessage(), e);
            }
        });
        log.info("Subscribed to Pub/Sub subscription: {}", SUBSCRIPTION);
    }

    private void processEvent(TransactionEventEnvelope envelope) throws Exception {
        String eventType = envelope.eventType();
        String dataJson = objectMapper.writeValueAsString(envelope.data());

        switch (eventType) {
            case "transaction-created" -> {
                TransactionCreatedEventData data = objectMapper.readValue(dataJson, TransactionCreatedEventData.class);
                Money amount = Money.of(data.amount(), Currency.valueOf(data.currency()));
                TransactionType type = TransactionType.valueOf(data.type());
                dailyBalanceService.applyTransaction(data.date(), amount, type);
            }
            case "transaction-reversed" -> {
                TransactionReversedEventData data = objectMapper.readValue(dataJson, TransactionReversedEventData.class);
                BigDecimal amount = data.amount();
                if (amount == null) {
                    log.warn("Received transaction-reversed event without amount, skipping: eventId={}", envelope.eventId());
                    return;
                }
                Money money = Money.of(amount, Currency.valueOf(data.currency()));
                TransactionType type = TransactionType.valueOf(data.type());
                dailyBalanceService.applyTransaction(data.date(), money, type);
            }
            default -> log.warn("Unknown event type: {}, eventId={}", eventType, envelope.eventId());
        }
    }
}
