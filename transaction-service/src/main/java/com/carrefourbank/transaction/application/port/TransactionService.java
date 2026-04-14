package com.carrefourbank.transaction.application.port;

import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.transaction.application.command.TransactionCreateCommand;
import com.carrefourbank.transaction.application.command.TransactionReversalCommand;
import com.carrefourbank.transaction.application.dto.GenerateReportRequest;
import com.carrefourbank.transaction.application.dto.ReportStatusResponse;
import com.carrefourbank.transaction.application.dto.TransactionDTO;
import com.carrefourbank.transaction.application.dto.TransactionPageResponse;

import java.time.LocalDate;
import java.util.UUID;

public interface TransactionService {

    TransactionDTO create(TransactionCreateCommand command);

    TransactionDTO findById(UUID id);

    TransactionPageResponse findAll(LocalDate startDate, LocalDate endDate, TransactionType type, int page, int size);

    TransactionDTO reverse(UUID id, TransactionReversalCommand command);

    ReportStatusResponse generateReport(GenerateReportRequest request);
}
