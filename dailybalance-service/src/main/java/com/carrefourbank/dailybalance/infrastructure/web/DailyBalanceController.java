package com.carrefourbank.dailybalance.infrastructure.web;

import com.carrefourbank.dailybalance.application.dto.CloseBalanceRequest;
import com.carrefourbank.dailybalance.application.dto.DailyBalanceDTO;
import com.carrefourbank.dailybalance.application.dto.DailyBalancePageResponse;
import com.carrefourbank.dailybalance.application.dto.ExportBalanceRequest;
import com.carrefourbank.dailybalance.application.dto.ExportStatusResponse;
import com.carrefourbank.dailybalance.application.dto.RecalculateResponse;
import com.carrefourbank.dailybalance.application.dto.ReopenBalanceRequest;
import com.carrefourbank.dailybalance.application.port.DailyBalanceService;
import com.carrefourbank.dailybalance.domain.model.BalanceStatus;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/dailybalances")
public class DailyBalanceController {

    private final DailyBalanceService service;

    public DailyBalanceController(DailyBalanceService service) {
        this.service = service;
    }

    @GetMapping("/{date}")
    public DailyBalanceDTO findByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.findByDate(date);
    }

    @GetMapping
    public DailyBalancePageResponse findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BalanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(startDate, endDate, status, page, size);
    }

    @PostMapping("/{date}/close")
    public DailyBalanceDTO closeBalance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody(required = false) CloseBalanceRequest request) {
        return service.closeBalance(date, request != null ? request : new CloseBalanceRequest(null));
    }

    @PostMapping("/{date}/reopen")
    public DailyBalanceDTO reopenBalance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ReopenBalanceRequest request) {
        return service.reopenBalance(date, request);
    }

    @PostMapping("/{date}/recalculate")
    public RecalculateResponse recalculate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.recalculate(date);
    }

    @PostMapping("/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExportStatusResponse exportToERP(@Valid @RequestBody ExportBalanceRequest request) {
        return service.exportToERP(request);
    }
}
