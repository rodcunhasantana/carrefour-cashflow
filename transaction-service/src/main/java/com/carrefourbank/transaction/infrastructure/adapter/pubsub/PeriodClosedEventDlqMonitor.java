package com.carrefourbank.transaction.infrastructure.adapter.pubsub;

import com.google.cloud.spring.pubsub.core.PubSubTemplate;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * Monitora a Dead Letter Queue da subscription de período fechado.
 * Mensagens chegam aqui após 5 tentativas malsucedidas de processamento.
 * Registra o payload em ERROR para triagem e confirma a mensagem para evitar
 * acúmulo indefinido na DLQ.
 */
@Component
@Profile("!test")
public class PeriodClosedEventDlqMonitor {

    private static final Logger log = LoggerFactory.getLogger(PeriodClosedEventDlqMonitor.class);
    private static final String DLQ_SUBSCRIPTION = "period-events-dlq-sub";

    private final PubSubTemplate pubSubTemplate;

    public PeriodClosedEventDlqMonitor(PubSubTemplate pubSubTemplate) {
        this.pubSubTemplate = pubSubTemplate;
    }

    @PostConstruct
    public void startDlqSubscription() {
        pubSubTemplate.subscribe(DLQ_SUBSCRIPTION, message -> {
            String payload = message.getPubsubMessage().getData().toStringUtf8();
            String messageId = message.getPubsubMessage().getMessageId();
            log.error(
                "DLQ: unprocessable period-closed event after max delivery attempts. " +
                "messageId={} payload={}",
                messageId, payload
            );
            message.ack();
        });
        log.info("DLQ monitor subscribed to: {}", DLQ_SUBSCRIPTION);
    }
}
