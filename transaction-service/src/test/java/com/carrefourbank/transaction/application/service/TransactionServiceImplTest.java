package com.carrefourbank.transaction.application.service;


import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.common.exception.NotFoundException;
import com.carrefourbank.transaction.application.command.TransactionCreateCommand;
import com.carrefourbank.transaction.application.command.TransactionReversalCommand;
import com.carrefourbank.transaction.application.dto.TransactionDTO;
import com.carrefourbank.transaction.application.dto.TransactionPageResponse;
import com.carrefourbank.transaction.application.mapper.TransactionMapper;
import com.carrefourbank.transaction.domain.exception.AlreadyReversedException;
import com.carrefourbank.transaction.domain.exception.PeriodClosedException;
import com.carrefourbank.transaction.domain.model.Transaction;
import com.carrefourbank.transaction.domain.model.TransactionStatus;
import com.carrefourbank.transaction.domain.port.ClosedPeriodRepository;
import com.carrefourbank.transaction.domain.port.TransactionEventPublisher;
import com.carrefourbank.transaction.domain.port.TransactionRepository;
import com.carrefourbank.transaction.infrastructure.logging.TransactionLogger;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import org.junit.jupiter.api.Tag;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@Tag("unit")
@Tag("service")
@Tag("FR-01")
@Tag("FR-02")
@Tag("FR-03")
@Tag("FR-04")
@Tag("FR-05")
@ExtendWith(MockitoExtension.class)
class TransactionServiceImplTest {

    @Mock private TransactionRepository repository;
    @Mock private ClosedPeriodRepository closedPeriodRepository;
    @Mock private TransactionEventPublisher eventPublisher;
    @Mock private TransactionLogger logger;

    private TransactionServiceImpl service;
    private TransactionMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TransactionMapper();
        service = new TransactionServiceImpl(repository, closedPeriodRepository, eventPublisher, mapper, logger);
    }

    @Test
    void create_saves_and_publishes_event() {
        TransactionCreateCommand command = new TransactionCreateCommand(
                TransactionType.CREDIT, new BigDecimal("100.00"), LocalDate.now(), "Test");
        Transaction saved = Transaction.create(TransactionType.CREDIT,
                Money.ofBRL(new BigDecimal("100.00")), LocalDate.now(), "Test");
        when(closedPeriodRepository.isDateClosed(command.date())).thenReturn(false);
        when(repository.save(any())).thenReturn(saved);

        TransactionDTO result = service.create(command);

        assertNotNull(result);
        verify(repository).save(any());
        verify(eventPublisher).publishTransactionCreatedEvent(saved);
        verify(logger).logCreated(saved);
    }

    @Test
    void create_throws_period_closed_when_date_is_closed() {
        LocalDate closedDate = LocalDate.of(2026, 4, 10);
        TransactionCreateCommand command = new TransactionCreateCommand(
                TransactionType.CREDIT, new BigDecimal("100.00"), closedDate, "Late entry");
        when(closedPeriodRepository.isDateClosed(closedDate)).thenReturn(true);

        assertThrows(PeriodClosedException.class, () -> service.create(command));
        verify(repository, never()).save(any());
        verify(eventPublisher, never()).publishTransactionCreatedEvent(any());
    }

    @Test
    void findById_returns_dto_when_found() {
        Transaction tx = Transaction.create(TransactionType.CREDIT,
                Money.ofBRL(new BigDecimal("50.00")), LocalDate.now(), "Found");
        when(repository.findById(tx.id())).thenReturn(Optional.of(tx));

        TransactionDTO result = service.findById(tx.id());

        assertEquals(tx.id().toString(), result.id());
    }

    @Test
    void findById_throws_not_found_when_missing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class, () -> service.findById(id));
    }

    @Test
    void findAll_returns_page_response() {
        Transaction tx = Transaction.create(TransactionType.CREDIT,
                Money.ofBRL(new BigDecimal("75.00")), LocalDate.now(), "List test");
        when(repository.findAll(null, null, null, 0, 20)).thenReturn(List.of(tx));
        when(repository.count(null, null, null)).thenReturn(1L);

        TransactionPageResponse response = service.findAll(null, null, null, 0, 20);

        assertEquals(1, response.content().size());
        assertEquals(1L, response.pageable().totalElements());
    }

    @Test
    void reverse_creates_reversal_and_marks_original_reversed() {
        Transaction original = Transaction.create(TransactionType.CREDIT,
                Money.ofBRL(new BigDecimal("100.00")), LocalDate.now(), "Original");
        when(repository.findById(original.id())).thenReturn(Optional.of(original));
        when(repository.existsReversalFor(original.id())).thenReturn(false);
        when(repository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TransactionDTO result = service.reverse(original.id(), new TransactionReversalCommand("Wrong entry"));

        assertTrue(result.reversal());
        verify(repository, times(2)).save(any());
        verify(eventPublisher).publishTransactionReversedEvent(any(), any());
    }

    @Test
    void reverse_throws_already_reversed_when_reversal_exists() {
        Transaction original = Transaction.create(TransactionType.CREDIT,
                Money.ofBRL(new BigDecimal("100.00")), LocalDate.now(), "Already reversed");
        when(repository.findById(original.id())).thenReturn(Optional.of(original));
        when(repository.existsReversalFor(original.id())).thenReturn(true);

        assertThrows(AlreadyReversedException.class,
                () -> service.reverse(original.id(), new TransactionReversalCommand("Again")));
    }

    @Test
    void reverse_throws_not_found_when_transaction_missing() {
        UUID id = UUID.randomUUID();
        when(repository.findById(id)).thenReturn(Optional.empty());
        assertThrows(NotFoundException.class,
                () -> service.reverse(id, new TransactionReversalCommand("Reason")));
    }
}
