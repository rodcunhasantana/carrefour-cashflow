package com.carrefourbank.dailybalance.application.port;

import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.dailybalance.application.dto.CloseBalanceRequest;
import com.carrefourbank.dailybalance.application.dto.DailyBalanceDTO;
import com.carrefourbank.dailybalance.application.dto.DailyBalancePageResponse;
import com.carrefourbank.dailybalance.application.dto.DailyBalanceTransactionDTO;
import com.carrefourbank.dailybalance.application.dto.ExportBalanceRequest;
import com.carrefourbank.dailybalance.application.dto.ExportStatusResponse;
import com.carrefourbank.dailybalance.application.dto.RecalculateResponse;
import com.carrefourbank.dailybalance.application.dto.ReopenBalanceRequest;
import com.carrefourbank.dailybalance.domain.model.BalanceStatus;

import java.time.LocalDate;
import java.util.List;

public interface DailyBalanceService {
    DailyBalanceDTO findByDate(LocalDate date);
    DailyBalancePageResponse findAll(LocalDate startDate, LocalDate endDate, BalanceStatus status, int page, int size);
    DailyBalanceDTO closeBalance(LocalDate date, CloseBalanceRequest request);
    DailyBalanceDTO reopenBalance(LocalDate date, ReopenBalanceRequest request);
    RecalculateResponse recalculate(LocalDate date);
    ExportStatusResponse exportToERP(ExportBalanceRequest request);
    void applyTransaction(String eventId, String transactionId, LocalDate date, Money amount, TransactionType type);
    List<DailyBalanceTransactionDTO> findTransactionsByDate(LocalDate date);
}
