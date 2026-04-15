package com.carrefourbank.dailybalance.domain.port;

import com.carrefourbank.dailybalance.domain.model.DailyBalanceTransaction;

import java.util.List;
import java.util.UUID;

public interface DailyBalanceTransactionRepository {
    void save(DailyBalanceTransaction entry);
    List<DailyBalanceTransaction> findByBalanceId(UUID balanceId);
}
