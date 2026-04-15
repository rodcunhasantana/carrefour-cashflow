package com.carrefourbank.transaction.infrastructure.adapter.pubsub;

import com.carrefourbank.transaction.domain.port.ClosedPeriodRepository;
import com.carrefourbank.transaction.infrastructure.adapter.pubsub.event.PeriodClosedEventData;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Component
@Profile("!test")
public class PeriodClosedEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PeriodClosedEventConsumer.class);
    private static final String SUBSCRIPTION = "transaction-period-subscription";

    private final PubSubTemplate pubSubTemplate;
    private final ObjectMapper objectMapper;
    private final ClosedPeriodRepository closedPeriodRepository;

    public PeriodClosedEventConsumer(
            PubSubTemplate pubSubTemplate,
            ObjectMapper objectMapper,
            ClosedPeriodRepository closedPeriodRepository) {
        this.pubSubTemplate = pubSubTemplate;
        this.objectMapper = objectMapper;
        this.closedPeriodRepository = closedPeriodRepository;
    }

    @PostConstruct
    public void startSubscription() {
        pubSubTemplate.subscribe(SUBSCRIPTION, message -> {
            String payload = message.getPubsubMessage().getData().toStringUtf8();
            try {
                JsonNode envelope = objectMapper.readTree(payload);
                String eventType = envelope.get("eventType").asText();
                if ("period-closed".equals(eventType)) {
                    PeriodClosedEventData data = objectMapper.treeToValue(
                            envelope.get("data"), PeriodClosedEventData.class);
                    closedPeriodRepository.closeDate(data.date());
                    log.info("Period closed registered: date={}", data.date());
                } else {
                    log.warn("Unknown event type on period subscription: {}", eventType);
                }
                message.ack();
            } catch (Exception e) {
                log.error("Failed to process period-closed event: {}", e.getMessage(), e);
            }
        });
        log.info("Subscribed to Pub/Sub subscription: {}", SUBSCRIPTION);
    }
}
