package com.carrefourbank.dailybalance.infrastructure.adapter.persistence;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import com.carrefourbank.dailybalance.domain.model.DailyBalanceTransaction;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceRepository;
import com.carrefourbank.dailybalance.domain.port.DailyBalanceTransactionRepository;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Tag("integration")
@Tag("persistence")
@Tag("FR-11")
@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Rollback
class JdbcDailyBalanceTransactionRepositoryTest {

    @Autowired
    private DailyBalanceTransactionRepository auditRepository;

    @Autowired
    private DailyBalanceRepository balanceRepository;

    private static final Money OPENING = Money.of(new BigDecimal("1000.00"), Currency.BRL);

    @Test
    void save_and_findByBalanceId_roundTrip() {
        DailyBalance balance = balanceRepository.save(DailyBalance.create(LocalDate.of(2026, 6, 1), OPENING));
        Money credit = Money.of(new BigDecimal("200.00"), Currency.BRL);
        DailyBalanceTransaction entry = DailyBalanceTransaction.create(
                balance.id(), "tx-001", "evt-001", TransactionType.CREDIT, credit);

        auditRepository.save(entry);
        List<DailyBalanceTransaction> found = auditRepository.findByBalanceId(balance.id());

        assertEquals(1, found.size());
        assertEquals("tx-001", found.get(0).transactionId());
        assertEquals("evt-001", found.get(0).eventId());
        assertEquals(TransactionType.CREDIT, found.get(0).type());
        assertEquals(0, credit.amount().compareTo(found.get(0).amount().amount()));
    }

    @Test
    void findByBalanceId_returnsMultipleEntriesOrderedByAppliedAt() {
        DailyBalance balance = balanceRepository.save(DailyBalance.create(LocalDate.of(2026, 6, 2), OPENING));
        Money credit = Money.of(new BigDecimal("100.00"), Currency.BRL);
        Money debit  = Money.of(new BigDecimal("-50.00"), Currency.BRL);

        auditRepository.save(DailyBalanceTransaction.create(balance.id(), "tx-A", "evt-A", TransactionType.CREDIT, credit));
        auditRepository.save(DailyBalanceTransaction.create(balance.id(), "tx-B", "evt-B", TransactionType.DEBIT, debit));

        List<DailyBalanceTransaction> found = auditRepository.findByBalanceId(balance.id());

        assertEquals(2, found.size());
        assertEquals("tx-A", found.get(0).transactionId());
        assertEquals("tx-B", found.get(1).transactionId());
    }

    @Test
    void findByBalanceId_returnsEmpty_forUnknownBalance() {
        DailyBalance balance = DailyBalance.create(LocalDate.of(2099, 1, 1), OPENING);

        List<DailyBalanceTransaction> found = auditRepository.findByBalanceId(balance.id());

        assertTrue(found.isEmpty());
    }
}
