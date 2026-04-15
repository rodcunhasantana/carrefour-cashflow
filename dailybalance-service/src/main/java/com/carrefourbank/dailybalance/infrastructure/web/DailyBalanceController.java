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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
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

@Tag(name = "Daily Balances", description = "Saldos diários consolidados — consulta, fechamento e recálculo de períodos")
@RestController
@RequestMapping("/api/dailybalances")
public class DailyBalanceController {

    private final DailyBalanceService service;

    public DailyBalanceController(DailyBalanceService service) {
        this.service = service;
    }

    @Operation(summary = "Buscar saldo por data")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Saldo encontrado"),
        @ApiResponse(responseCode = "404", description = "Saldo não encontrado para a data")
    })
    @GetMapping("/{date}")
    public DailyBalanceDTO findByDate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.findByDate(date);
    }

    @Operation(summary = "Listar saldos", description = "Lista paginada com filtros opcionais por período e status.")
    @ApiResponse(responseCode = "200", description = "Lista de saldos")
    @GetMapping
    public DailyBalancePageResponse findAll(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            @RequestParam(required = false) BalanceStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return service.findAll(startDate, endDate, status, page, size);
    }

    @Operation(summary = "Fechar período", description = "Fecha o saldo do dia. Após fechar, novos lançamentos para essa data são rejeitados.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Período fechado"),
        @ApiResponse(responseCode = "404", description = "Saldo não encontrado"),
        @ApiResponse(responseCode = "409", description = "Período já fechado")
    })
    @PostMapping("/{date}/close")
    public DailyBalanceDTO closeBalance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestBody(required = false) CloseBalanceRequest request) {
        return service.closeBalance(date, request != null ? request : new CloseBalanceRequest(null));
    }

    @Operation(summary = "Reabrir período")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Período reaberto"),
        @ApiResponse(responseCode = "404", description = "Saldo não encontrado"),
        @ApiResponse(responseCode = "409", description = "Período já está aberto")
    })
    @PostMapping("/{date}/reopen")
    public DailyBalanceDTO reopenBalance(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @Valid @RequestBody ReopenBalanceRequest request) {
        return service.reopenBalance(date, request);
    }

    @Operation(summary = "Recalcular saldo", description = "Recalcula o saldo de fechamento: openingBalance + totalCredits + totalDebits.")
    @ApiResponses({
        @ApiResponse(responseCode = "200", description = "Saldo recalculado"),
        @ApiResponse(responseCode = "404", description = "Saldo não encontrado")
    })
    @PostMapping("/{date}/recalculate")
    public RecalculateResponse recalculate(
            @PathVariable @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return service.recalculate(date);
    }

    @Operation(summary = "Exportar para ERP (stub)", description = "Inicia exportação assíncrona do saldo para o ERP. Retorna ID para acompanhar status.")
    @ApiResponse(responseCode = "202", description = "Exportação em processamento")
    @PostMapping("/export")
    @ResponseStatus(HttpStatus.ACCEPTED)
    public ExportStatusResponse exportToERP(@Valid @RequestBody ExportBalanceRequest request) {
        return service.exportToERP(request);
    }
}
