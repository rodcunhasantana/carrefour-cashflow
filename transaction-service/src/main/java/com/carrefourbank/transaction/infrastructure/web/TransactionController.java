package com.carrefourbank.transaction.infrastructure.web;

import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.transaction.application.command.TransactionReversalCommand;
import com.carrefourbank.transaction.application.dto.CreateTransactionRequest;
import com.carrefourbank.transaction.application.dto.GenerateReportRequest;
import com.carrefourbank.transaction.application.dto.ReportStatusResponse;
import com.carrefourbank.transaction.application.dto.TransactionDTO;
import com.carrefourbank.transaction.application.dto.TransactionPageResponse;
import com.carrefourbank.transaction.application.mapper.TransactionMapper;
import com.carrefourbank.transaction.application.port.TransactionService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.UUID;

@RestController
@RequestMapping("/api/transactions")
public class TransactionController {

    private final TransactionService service;
    private final TransactionMapper mapper;

    public TransactionController(TransactionService service, TransactionMapper mapper) {
        this.service = service;
        this.mapper = mapper;
    }

    @PostMapping
    public ResponseEntity<TransactionDTO> create(@Valid @RequestBody CreateTransactionRequest request) {
        TransactionDTO dto = service.create(mapper.toCommand(request));
        return ResponseEntity.status(HttpStatus.CREATED).body(dto);
    }

    @GetMapping("/{id}")
    public ResponseEntity<TransactionDTO> findById(@PathVariable UUID id) {
        return ResponseEntity.ok(service.findById(id));
    }

    @GetMapping
    public ResponseEntity<TransactionPageResponse> findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) TransactionType type,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(service.findAll(startDate, endDate, type, page, size));
    }

    @PostMapping("/{id}/reverse")
    public ResponseEntity<TransactionDTO> reverse(
            @PathVariable UUID id,
            @Valid @RequestBody com.carrefourbank.transaction.application.dto.ReverseTransactionRequest request) {
        TransactionDTO dto = service.reverse(id, new TransactionReversalCommand(request.reason()));
        return ResponseEntity.ok(dto);
    }

    @PostMapping("/reports")
    public ResponseEntity<ReportStatusResponse> generateReport(@Valid @RequestBody GenerateReportRequest request) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(service.generateReport(request));
    }
}
