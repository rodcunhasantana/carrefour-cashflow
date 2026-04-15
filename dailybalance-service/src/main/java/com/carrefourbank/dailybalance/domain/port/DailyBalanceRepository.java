package com.carrefourbank.dailybalance.domain.port;

import com.carrefourbank.dailybalance.domain.model.BalanceStatus;
import com.carrefourbank.dailybalance.domain.model.DailyBalance;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface DailyBalanceRepository {
    DailyBalance save(DailyBalance balance);
    Optional<DailyBalance> findById(UUID id);
    Optional<DailyBalance> findByDate(LocalDate date);
    Optional<DailyBalance> findMostRecentClosedBefore(LocalDate date);
    List<DailyBalance> findAll(LocalDate startDate, LocalDate endDate, BalanceStatus status, int page, int size);
    long count(LocalDate startDate, LocalDate endDate, BalanceStatus status);
}
