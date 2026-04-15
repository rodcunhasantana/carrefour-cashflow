package com.carrefourbank.dailybalance.application.service;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.dailybalance.application.dto.CloseBalanceRequest;
import com.carrefourbank.dailybalance.application.dto.ReopenBalanceRequest;
import com.carrefourbank.dailybalance.application.port.DailyBalanceService;
import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import com.carrefourbank.dailybalance.domain.port.BalanceEventPublisher;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceRepository;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceTransactionRepository;
import com.carrefourbank.dailybalance.domain.port.ProcessedEventRepository;
import com.carrefourbank.dailybalance.infrastructure.logging.DailyBalanceLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.cache.CacheManager;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("integration")
@Tag("service")
@Tag("NFR-08")
@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
        "spring.cache.type=caffeine",
        "spring.cache.caffeine.spec=maximumSize=10,expireAfterWrite=1m",
        "spring.cache.cache-names=dailyBalances"
})
class DailyBalanceCacheTest {

    private static final LocalDate DATE = LocalDate.of(2025, 1, 20);
    private static final Money OPENING = Money.of(new BigDecimal("1000.00"), Currency.BRL);

    @Autowired
    private DailyBalanceService service;

    @Autowired
    private CacheManager cacheManager;

    @MockBean
    private DailyBalanceRepository repository;

    @MockBean
    private ProcessedEventRepository processedEventRepository;

    @MockBean
    private BalanceEventPublisher balanceEventPublisher;

    @MockBean
    private DailyBalanceTransactionRepository auditRepository;

    @MockBean
    private DailyBalanceLogger logger;

    @BeforeEach
    void clearCache() {
        cacheManager.getCache("dailyBalances").clear();
    }

    @Test
    void findByDate_hitsCache_onSecondCall() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        when(repository.findByDate(DATE)).thenReturn(Optional.of(balance));

        service.findByDate(DATE); // cache miss — hits repository
        service.findByDate(DATE); // cache hit — repository NOT called again

        verify(repository, times(1)).findByDate(DATE);
    }

    @Test
    void closeBalance_evictsCache() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        when(repository.findByDate(DATE)).thenReturn(Optional.of(balance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.findByDate(DATE);                                    // populates cache
        service.closeBalance(DATE, new CloseBalanceRequest(null));   // evicts cache
        service.findByDate(DATE);                                    // cache miss again

        // findByDate called: (1) initial population, (2) after eviction
        // closeBalance calls findBalanceByDate internally (direct repository call, bypasses cache proxy)
        verify(repository, times(3)).findByDate(DATE);
    }

    @Test
    void reopenBalance_evictsCache() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING).close();
        when(repository.findByDate(DATE)).thenReturn(Optional.of(balance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.findByDate(DATE);                                    // populates cache
        service.reopenBalance(DATE, new ReopenBalanceRequest("correction", "admin")); // evicts cache
        service.findByDate(DATE);                                    // cache miss again

        verify(repository, times(3)).findByDate(DATE);
    }

    @Test
    void applyTransaction_evictsCache() {
        DailyBalance balance = DailyBalance.create(DATE, OPENING);
        Money credit = Money.of(new BigDecimal("500.00"), Currency.BRL);
        when(processedEventRepository.markAsProcessed("evt-cache")).thenReturn(true);
        when(repository.findByDate(DATE)).thenReturn(Optional.of(balance));
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        service.findByDate(DATE);                                                        // populates cache
        service.applyTransaction("evt-cache", "tx-1", DATE, credit, TransactionType.CREDIT); // evicts cache
        service.findByDate(DATE);                                                        // cache miss again

        // (1) initial cache population, (2) inside applyTransaction via findOrCreateForDate, (3) after eviction
        verify(repository, times(3)).findByDate(DATE);
    }
}
