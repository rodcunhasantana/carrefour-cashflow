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
import com.carrefourbank.dailybalance.application.mapper.DailyBalanceMapper;
import com.carrefourbank.dailybalance.domain.exception.BalanceAlreadyClosedException;
import com.carrefourbank.dailybalance.domain.model.BalanceStatus;
import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import com.carrefourbank.dailybalance.domain.port.BalanceEventPublisher;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceRepository;
import com.carrefourbank.dailybalance.domain.port.ProcessedEventRepository;
import com.carrefourbank.dailybalance.infrastructure.logging.DailyBalanceLogger;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class DailyBalanceServiceImplTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 14);
    private static final Money OPENING = Money.of(new BigDecimal("1000.00"), Currency.BRL);

    @Mock
    private DailyBalanceRepository repository;

    @Mock
    private ProcessedEventRepository processedEventRepository;

    @Mock
    private BalanceEventPublisher balanceEventPublisher;

    @Mock
    private DailyBalanceLogger logger;

    @Spy
    private DailyBalanceMapper mapper;

    @InjectMocks
    private DailyBalanceServiceImpl service;

    @Test
    void findByDate_returnsDTO_whenFound() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        when(repository.findByDate(DATE)).thenReturn(Optional.of(balance));

        DailyBalanceDTO dto = service.findByDate(DATE);

        assertNotNull(dto);
        assertEquals(DATE, dto.date());
        assertEquals(BalanceStatus.OPEN, dto.status());
    }

    @Test
    void findByDate_throwsNotFoundException_whenNotFound() {
        when(repository.findByDate(DATE)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> service.findByDate(DATE));
    }

    @Test
    void findAll_returnsPageResponse() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        when(repository.findAll(null, null, null, 0, 20)).thenReturn(List.of(balance));
        when(repository.count(null, null, null)).thenReturn(1L);

        DailyBalancePageResponse response = service.findAll(null, null, null, 0, 20);

        assertEquals(1, response.content().size());
        assertEquals(1L, response.pageable().totalElements());
        assertEquals(1, response.pageable().totalPages());
    }

    @Test
    void closeBalance_closesAndSaves() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        when(repository.findByDate(DATE)).thenReturn(Optional.of(balance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        DailyBalanceDTO dto = service.closeBalance(DATE, new CloseBalanceRequest(null));

        assertEquals(BalanceStatus.CLOSED, dto.status());
        verify(repository).save(any(DailyBalance.class));
        verify(logger).logClosed(any(DailyBalance.class));
    }

    @Test
    void closeBalance_publishesPeriodClosedEvent() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        when(repository.findByDate(DATE)).thenReturn(Optional.of(balance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.closeBalance(DATE, new CloseBalanceRequest(null));

        verify(balanceEventPublisher).publishPeriodClosedEvent(any(DailyBalance.class));
    }

    @Test
    void closeBalance_propagatesBalanceAlreadyClosedException() {
        DailyBalance closed = DailyBalance.create(DATE, OPENING).close();
        when(repository.findByDate(DATE)).thenReturn(Optional.of(closed));

        assertThrows(BalanceAlreadyClosedException.class,
                () -> service.closeBalance(DATE, new CloseBalanceRequest(null)));
    }

    @Test
    void recalculate_returnsRecalculateResponseWithDetails() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING)
                .withAddedCredit(Money.of(new BigDecimal("200.00"), Currency.BRL))
                .withAddedDebit(Money.of(new BigDecimal("-50.00"), Currency.BRL));
        when(repository.findByDate(DATE)).thenReturn(Optional.of(balance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        RecalculateResponse response = service.recalculate(DATE);

        assertNotNull(response.balance());
        assertNotNull(response.recalculationDetails());
        assertEquals("200.00", response.recalculationDetails().previousCredits());
        assertEquals("-50.00", response.recalculationDetails().previousDebits());
        verify(logger).logRecalculated(any(DailyBalance.class));
    }

    @Test
    void exportToERP_returnsProcessingStatus() {
        ExportBalanceRequest request = new ExportBalanceRequest(DATE, DATE.plusDays(7), null);

        ExportStatusResponse response = service.exportToERP(request);

        assertEquals("PROCESSING", response.status());
        assertNotNull(response.exportId());
        assertNotNull(response.statusCheckUrl());
    }

    @Test
    void applyTransaction_credit_createsNewBalanceWhenNotExists() {
        Money credit = Money.of(new BigDecimal("500.00"), Currency.BRL);
        when(processedEventRepository.markAsProcessed("evt-1")).thenReturn(true);
        when(repository.findByDate(DATE)).thenReturn(Optional.empty());
        when(repository.findMostRecentClosedBefore(DATE)).thenReturn(Optional.empty());
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.applyTransaction("evt-1", DATE, credit, TransactionType.CREDIT);

        verify(repository, times(2)).save(any(DailyBalance.class));
    }

    @Test
    void applyTransaction_debit_appliesOnExistingBalance() {
        DailyBalance existing = DailyBalance.create(DATE, OPENING);
        Money debit = Money.of(new BigDecimal("-200.00"), Currency.BRL);
        when(processedEventRepository.markAsProcessed("evt-2")).thenReturn(true);
        when(repository.findByDate(DATE)).thenReturn(Optional.of(existing));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.applyTransaction("evt-2", DATE, debit, TransactionType.DEBIT);

        verify(repository).save(argThat(b -> b.totalDebits().amount().compareTo(new BigDecimal("-200.00")) == 0));
    }

    @Test
    void applyTransaction_duplicate_eventId_skipsProcessing() {
        Money credit = Money.of(new BigDecimal("500.00"), Currency.BRL);
        when(processedEventRepository.markAsProcessed("evt-dup")).thenReturn(false);

        service.applyTransaction("evt-dup", DATE, credit, TransactionType.CREDIT);

        verify(repository, never()).findByDate(any());
        verify(repository, never()).save(any());
    }
}
