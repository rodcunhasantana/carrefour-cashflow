package com.carrefourbank.dailybalance.infrastructure.adapter.pubsub;

import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import com.carrefourbank.dailybalance.domain.port.BalanceEventPublisher;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Component
@Profile("test")
public class NoOpBalanceEventPublisher implements BalanceEventPublisher {

    @Override
    public void publishPeriodClosedEvent(DailyBalance closedBalance) {
        // no-op in test profile
    }
}
