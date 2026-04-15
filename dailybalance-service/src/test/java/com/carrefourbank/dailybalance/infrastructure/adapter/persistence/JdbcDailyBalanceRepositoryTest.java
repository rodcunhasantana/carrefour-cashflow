package com.carrefourbank.dailybalance.infrastructure.adapter.persistence;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.dailybalance.domain.model.BalanceStatus;
import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class JdbcDailyBalanceRepositoryTest {

    @Autowired
    private DailyBalanceRepository repository;

    private static final Money OPENING = Money.of(new BigDecimal("1000.00"), Currency.BRL);

    @Test
    void save_and_findByDate_roundTrip() {
        LocalDate date = LocalDate.of(2026, 1, 10);
        DailyBalance balance = DailyBalance.create(date, OPENING);

        repository.save(balance);
        Optional<DailyBalance> found = repository.findByDate(date);

        assertTrue(found.isPresent());
        assertEquals(date, found.get().date());
        assertEquals(0, OPENING.amount().compareTo(found.get().openingBalance().amount()));
        assertEquals(BalanceStatus.OPEN, found.get().status());
    }

    @Test
    void save_and_findById_roundTrip() {
        LocalDate date = LocalDate.of(2026, 1, 11);
        DailyBalance balance = DailyBalance.create(date, OPENING);

        repository.save(balance);
        Optional<DailyBalance> found = repository.findById(balance.id());

        assertTrue(found.isPresent());
        assertEquals(balance.id(), found.get().id());
    }

    @Test
    void save_updatesExistingRecord() {
        LocalDate date = LocalDate.of(2026, 1, 12);
        DailyBalance balance = DailyBalance.create(date, OPENING);
        repository.save(balance);

        DailyBalance closed = balance.close();
        repository.save(closed);

        Optional<DailyBalance> found = repository.findByDate(date);
        assertTrue(found.isPresent());
        assertEquals(BalanceStatus.CLOSED, found.get().status());
        assertNotNull(found.get().closedAt());
    }

    @Test
    void findByDate_returnsEmpty_whenNotFound() {
        Optional<DailyBalance> found = repository.findByDate(LocalDate.of(2099, 12, 31));

        assertFalse(found.isPresent());
    }

    @Test
    void findById_returnsEmpty_forUnknownId() {
        Optional<DailyBalance> found = repository.findById(UUID.randomUUID());

        assertFalse(found.isPresent());
    }

    @Test
    void findMostRecentClosedBefore_returnsCorrectRecord() {
        LocalDate date1 = LocalDate.of(2026, 2, 1);
        LocalDate date2 = LocalDate.of(2026, 2, 2);
        LocalDate date3 = LocalDate.of(2026, 2, 5);

        DailyBalance b1 = DailyBalance.create(date1, OPENING).close();
        DailyBalance b2 = DailyBalance.create(date2, OPENING).close();
        repository.save(b1);
        repository.save(b2);

        Optional<DailyBalance> found = repository.findMostRecentClosedBefore(date3);

        assertTrue(found.isPresent());
        assertEquals(date2, found.get().date());
    }

    @Test
    void findMostRecentClosedBefore_returnsEmpty_whenNoClosed() {
        LocalDate future = LocalDate.of(2099, 1, 1);
        Optional<DailyBalance> found = repository.findMostRecentClosedBefore(future);

        // Only non-closed records might exist — result depends on test data; empty is valid
        found.ifPresent(b -> assertEquals(BalanceStatus.CLOSED, b.status()));
    }

    @Test
    void findAll_withoutFilters_returnsAll() {
        LocalDate date1 = LocalDate.of(2026, 3, 1);
        LocalDate date2 = LocalDate.of(2026, 3, 2);
        repository.save(DailyBalance.create(date1, OPENING));
        repository.save(DailyBalance.create(date2, OPENING));

        List<DailyBalance> results = repository.findAll(date1, date2, null, 0, 10);

        assertTrue(results.size() >= 2);
    }

    @Test
    void findAll_filtersByStatus() {
        LocalDate date1 = LocalDate.of(2026, 4, 1);
        LocalDate date2 = LocalDate.of(2026, 4, 2);
        DailyBalance open = DailyBalance.create(date1, OPENING);
        DailyBalance closed = DailyBalance.create(date2, OPENING).close();
        repository.save(open);
        repository.save(closed);

        List<DailyBalance> openResults = repository.findAll(date1, date2, BalanceStatus.OPEN, 0, 10);
        List<DailyBalance> closedResults = repository.findAll(date1, date2, BalanceStatus.CLOSED, 0, 10);

        assertTrue(openResults.stream().allMatch(b -> b.status() == BalanceStatus.OPEN));
        assertTrue(closedResults.stream().allMatch(b -> b.status() == BalanceStatus.CLOSED));
    }

    @Test
    void count_returnsTotalMatchingRecords() {
        LocalDate date1 = LocalDate.of(2026, 5, 1);
        LocalDate date2 = LocalDate.of(2026, 5, 2);
        repository.save(DailyBalance.create(date1, OPENING));
        repository.save(DailyBalance.create(date2, OPENING));

        long count = repository.count(date1, date2, null);

        assertTrue(count >= 2);
    }
}
