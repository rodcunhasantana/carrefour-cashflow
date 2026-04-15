package com.carrefourbank.dailybalance.infrastructure.adapter.pubsub;

import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import com.carrefourbank.dailybalance.domain.port.BalanceEventPublisher;
import com.carrefourbank.dailybalance.infrastructure.adapter.pubsub.event.PeriodClosedEventData;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import io.github.resilience4j.retry.annotation.Retry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.Map;
import java.util.UUID;

@Component
@Profile("!test")
public class PubSubBalanceEventPublisher implements BalanceEventPublisher {

    private static final Logger log = LoggerFactory.getLogger(PubSubBalanceEventPublisher.class);
    private static final String TOPIC = "period-events";
    private static final String PRODUCER = "dailybalance-service";

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;

    public PubSubBalanceEventPublisher(PubSubTemplate pubSubTemplate, ObjectMapper objectMapper) {
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    @Retry(name = "default")
    @CircuitBreaker(name = "default")
    public void publishPeriodClosedEvent(DailyBalance closedBalance) {
        PeriodClosedEventData data = new PeriodClosedEventData(
                closedBalance.id().toString(),
                closedBalance.date());
        Map<String, Object> envelope = Map.of(
                "eventId", UUID.randomUUID().toString(),
                "eventType", "period-closed",
                "timestamp", Instant.now().toString(),
                "producer", PRODUCER,
                "data", data);
        try {
            String payload = objectMapper.writeValueAsString(envelope);
            pubSubTemplate.publish(TOPIC, payload);
            log.debug("Published event: type=period-closed topic={} date={}", TOPIC, closedBalance.date());
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Failed to serialize period-closed event", e);
        }
    }
}
