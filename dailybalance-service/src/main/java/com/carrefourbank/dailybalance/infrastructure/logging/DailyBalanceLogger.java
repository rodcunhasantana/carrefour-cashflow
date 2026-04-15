package com.carrefourbank.dailybalance.infrastructure.logging;

import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;
import org.springframework.stereotype.Component;

@Component
public class DailyBalanceLogger {

    private static final Logger log = LoggerFactory.getLogger(DailyBalanceLogger.class);
    private static final String MDC_KEY = "balanceId";

    public void logClosed(DailyBalance balance) {
        try {
            MDC.put(MDC_KEY, balance.id().toString());
            log.info("Daily balance closed: date={} closingBalance={}", balance.date(), balance.closingBalance().amount());
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    public void logReopened(DailyBalance balance) {
        try {
            MDC.put(MDC_KEY, balance.id().toString());
            log.info("Daily balance reopened: date={}", balance.date());
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    public void logRecalculated(DailyBalance balance) {
        try {
            MDC.put(MDC_KEY, balance.id().toString());
            log.info("Daily balance recalculated: date={} newClosingBalance={}", balance.date(), balance.closingBalance().amount());
        } finally {
            MDC.remove(MDC_KEY);
        }
    }
}
