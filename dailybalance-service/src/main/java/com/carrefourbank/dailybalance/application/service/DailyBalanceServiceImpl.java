package com.carrefourbank.dailybalance.application.service;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.common.exception.NotFoundException;
import com.carrefourbank.dailybalance.application.dto.CloseBalanceRequest;
import com.carrefourbank.dailybalance.application.dto.DailyBalanceDTO;
import com.carrefourbank.dailybalance.application.dto.DailyBalancePageResponse;
import com.carrefourbank.dailybalance.application.dto.ExportBalanceRequest;
import com.carrefourbank.dailybalance.application.dto.ExportStatusResponse;
import com.carrefourbank.dailybalance.application.dto.RecalculateResponse;
import com.carrefourbank.dailybalance.application.dto.ReopenBalanceRequest;
import com.carrefourbank.dailybalance.application.mapper.DailyBalanceMapper;
import com.carrefourbank.dailybalance.application.port.DailyBalanceService;
import com.carrefourbank.dailybalance.domain.model.BalanceStatus;
import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import com.carrefourbank.dailybalance.domain.port.BalanceEventPublisher;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceRepository;
import com.carrefourbank.dailybalance.domain.port.ProcessedEventRepository;
import com.carrefourbank.dailybalance.infrastructure.logging.DailyBalanceLogger;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class DailyBalanceServiceImpl implements DailyBalanceService {

    private static final Logger log = LoggerFactory.getLogger(DailyBalanceServiceImpl.class);

    private final DailyBalanceRepository repository;
    private final ProcessedEventRepository processedEventRepository;
    private final BalanceEventPublisher balanceEventPublisher;
    private final DailyBalanceMapper mapper;
    private final DailyBalanceLogger logger;

    public DailyBalanceServiceImpl(
            DailyBalanceRepository repository,
            ProcessedEventRepository processedEventRepository,
            BalanceEventPublisher balanceEventPublisher,
            DailyBalanceMapper mapper,
            DailyBalanceLogger logger) {
        this.repository = repository;
        this.processedEventRepository = processedEventRepository;
        this.balanceEventPublisher = balanceEventPublisher;
        this.mapper = mapper;
        this.logger = logger;
    }

    @Override
    @Transactional(readOnly = true)
    public DailyBalanceDTO findByDate(LocalDate date) {
        return repository.findByDate(date)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Daily balance not found for date: " + date));
    }

    @Override
    @Transactional(readOnly = true)
    public DailyBalancePageResponse findAll(LocalDate startDate, LocalDate endDate, BalanceStatus status, int page, int size) {
        List<DailyBalanceDTO> content = repository.findAll(startDate, endDate, status, page, size)
                .stream()
                .map(mapper::toDTO)
                .toList();
        long total = repository.count(startDate, endDate, status);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        DailyBalancePageResponse.PageableInfo pageable = new DailyBalancePageResponse.PageableInfo(page, size, total, totalPages);
        return new DailyBalancePageResponse(content, pageable);
    }

    @Override
    public DailyBalanceDTO closeBalance(LocalDate date, CloseBalanceRequest request) {
        DailyBalance balance = findBalanceByDate(date);
        DailyBalance closed = balance.close();
        DailyBalance saved = repository.save(closed);
        balanceEventPublisher.publishPeriodClosedEvent(saved);
        logger.logClosed(saved);
        return mapper.toDTO(saved);
    }

    @Override
    public DailyBalanceDTO reopenBalance(LocalDate date, ReopenBalanceRequest request) {
        DailyBalance balance = findBalanceByDate(date);
        DailyBalance reopened = balance.reopen();
        DailyBalance saved = repository.save(reopened);
        logger.logReopened(saved);
        return mapper.toDTO(saved);
    }

    @Override
    public RecalculateResponse recalculate(LocalDate date) {
        DailyBalance balance = findBalanceByDate(date);
        String prevCredits = balance.totalCredits().amount().toPlainString();
        String prevDebits = balance.totalDebits().amount().toPlainString();
        String prevClosing = balance.closingBalance().amount().toPlainString();

        DailyBalance recalculated = balance.recalculate();
        DailyBalance saved = repository.save(recalculated);
        logger.logRecalculated(saved);

        RecalculateResponse.RecalculationDetails details = new RecalculateResponse.RecalculationDetails(
                prevCredits, prevDebits, prevClosing,
                saved.closingBalance().amount().toPlainString());
        return new RecalculateResponse(mapper.toDTO(saved), details);
    }

    @Override
    public ExportStatusResponse exportToERP(ExportBalanceRequest request) {
        String exportId = UUID.randomUUID().toString();
        String statusCheckUrl = "/api/dailybalances/export/" + exportId;
        return new ExportStatusResponse(exportId, "PROCESSING", statusCheckUrl, LocalDateTime.now().plusMinutes(5));
    }

    @Override
    public void applyTransaction(String eventId, LocalDate date, Money amount, TransactionType type) {
        if (!processedEventRepository.markAsProcessed(eventId)) {
            log.warn("Duplicate event skipped: eventId={}", eventId);
            return;
        }
        DailyBalance balance = findOrCreateForDate(date);
        DailyBalance updated = type == TransactionType.CREDIT
                ? balance.withAddedCredit(amount)
                : balance.withAddedDebit(amount);
        repository.save(updated);
    }

    private DailyBalance findBalanceByDate(LocalDate date) {
        return repository.findByDate(date)
                .orElseThrow(() -> new NotFoundException("Daily balance not found for date: " + date));
    }

    private DailyBalance findOrCreateForDate(LocalDate date) {
        return repository.findByDate(date).orElseGet(() -> {
            Money openingBalance = repository.findMostRecentClosedBefore(date)
                    .map(DailyBalance::closingBalance)
                    .orElse(Money.zero(Currency.BRL));
            return repository.save(DailyBalance.create(date, openingBalance));
        });
    }
}
