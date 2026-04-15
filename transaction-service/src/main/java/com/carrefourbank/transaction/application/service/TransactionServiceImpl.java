package com.carrefourbank.transaction.application.service;

import com.carrefourbank.common.domain.Currency;
import com.carrefourbank.common.domain.Money;
import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.common.exception.NotFoundException;
import com.carrefourbank.transaction.application.command.TransactionCreateCommand;
import com.carrefourbank.transaction.application.command.TransactionReversalCommand;
import com.carrefourbank.transaction.application.dto.GenerateReportRequest;
import com.carrefourbank.transaction.application.dto.ReportStatusResponse;
import com.carrefourbank.transaction.application.dto.TransactionDTO;
import com.carrefourbank.transaction.application.dto.TransactionPageResponse;
import com.carrefourbank.transaction.application.mapper.TransactionMapper;
import com.carrefourbank.transaction.application.port.TransactionService;
import com.carrefourbank.transaction.domain.exception.AlreadyReversedException;
import com.carrefourbank.transaction.domain.exception.PeriodClosedException;
import com.carrefourbank.transaction.domain.model.Transaction;
import com.carrefourbank.transaction.domain.model.TransactionStatus;
import com.carrefourbank.transaction.domain.port.ClosedPeriodRepository;
import com.carrefourbank.transaction.domain.port.TransactionEventPublisher;
import com.carrefourbank.transaction.domain.port.TransactionRepository;
import com.carrefourbank.transaction.infrastructure.logging.TransactionLogger;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@Transactional
public class TransactionServiceImpl implements TransactionService {

    private final TransactionRepository repository;
    private final ClosedPeriodRepository closedPeriodRepository;
    private final TransactionEventPublisher eventPublisher;
    private final TransactionMapper mapper;
    private final TransactionLogger logger;

    public TransactionServiceImpl(
            TransactionRepository repository,
            ClosedPeriodRepository closedPeriodRepository,
            TransactionEventPublisher eventPublisher,
            TransactionMapper mapper,
            TransactionLogger logger) {
        this.repository = repository;
        this.closedPeriodRepository = closedPeriodRepository;
        this.eventPublisher = eventPublisher;
        this.mapper = mapper;
        this.logger = logger;
    }

    @Override
    public TransactionDTO create(TransactionCreateCommand command) {
        if (closedPeriodRepository.isDateClosed(command.date())) {
            throw new PeriodClosedException(command.date().toString());
        }
        Money amount = Money.of(command.amount(), Currency.BRL);
        Transaction transaction = Transaction.create(command.type(), amount, command.date(), command.description());
        Transaction saved = repository.save(transaction);
        eventPublisher.publishTransactionCreatedEvent(saved);
        logger.logCreated(saved);
        return mapper.toDTO(saved);
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionDTO findById(UUID id) {
        return repository.findById(id)
                .map(mapper::toDTO)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
    }

    @Override
    @Transactional(readOnly = true)
    public TransactionPageResponse findAll(LocalDate startDate, LocalDate endDate, TransactionType type, int page, int size) {
        List<TransactionDTO> content = repository.findAll(startDate, endDate, type, page, size)
                .stream()
                .map(mapper::toDTO)
                .toList();
        long total = repository.count(startDate, endDate, type);
        int totalPages = size > 0 ? (int) Math.ceil((double) total / size) : 0;
        TransactionPageResponse.PageableInfo pageable = new TransactionPageResponse.PageableInfo(page, size, total, totalPages);
        return new TransactionPageResponse(content, pageable);
    }

    @Override
    public TransactionDTO reverse(UUID id, TransactionReversalCommand command) {
        Transaction original = repository.findById(id)
                .orElseThrow(() -> new NotFoundException("Transaction not found: " + id));
        if (repository.existsReversalFor(id)) {
            throw new AlreadyReversedException(id.toString());
        }
        Transaction reversal = Transaction.createReversal(original, command.reason());
        Transaction savedReversal = repository.save(reversal);
        repository.save(original.withStatus(TransactionStatus.REVERSED));
        eventPublisher.publishTransactionReversedEvent(original, savedReversal);
        logger.logReversed(original, savedReversal);
        return mapper.toDTO(savedReversal);
    }

    @Override
    public ReportStatusResponse generateReport(GenerateReportRequest request) {
        String reportId = UUID.randomUUID().toString();
        String statusCheckUrl = "/api/transactions/reports/" + reportId;
        return new ReportStatusResponse(reportId, "PROCESSING", statusCheckUrl, LocalDateTime.now().plusMinutes(5));
    }
}
