package com.carrefourbank.dailybalance.domain.port;

import com.carrefourbank.dailybalance.domain.model.DailyBalance;

public interface BalanceEventPublisher {

    void publishPeriodClosedEvent(DailyBalance closedBalance);
}
