package com.carrefourbank.dailybalance.infrastructure.web;

import com.carrefourbank.common.exception.NotFoundException;
import com.carrefourbank.dailybalance.application.dto.CloseBalanceRequest;
import com.carrefourbank.dailybalance.application.dto.DailyBalanceDTO;
import com.carrefourbank.dailybalance.application.dto.DailyBalancePageResponse;
import com.carrefourbank.dailybalance.application.dto.DailyBalanceTransactionDTO;
import com.carrefourbank.dailybalance.application.dto.RecalculateResponse;
import com.carrefourbank.dailybalance.application.port.DailyBalanceService;
import com.carrefourbank.dailybalance.domain.exception.BalanceAlreadyClosedException;
import com.carrefourbank.dailybalance.domain.model.BalanceStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
class DailyBalanceControllerTest {

    private static final LocalDate DATE = LocalDate.of(2026, 4, 14);

    @Mock
    private DailyBalanceService service;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());
        DailyBalanceController controller = new DailyBalanceController(service);
        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();
    }

    private DailyBalanceDTO buildDTO(BalanceStatus status) {
        return new DailyBalanceDTO(
                UUID.randomUUID().toString(),
                DATE,
                "1000.00", "0.00", "0.00", "1000.00",
                "BRL", status,
                status == BalanceStatus.CLOSED ? LocalDateTime.now() : null,
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void findByDate_returns200_withDTO() throws Exception {
        when(service.findByDate(DATE)).thenReturn(buildDTO(BalanceStatus.OPEN));

        mockMvc.perform(get("/api/dailybalances/2026-04-14"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("OPEN"))
                .andExpect(jsonPath("$.openingBalance").value("1000.00"));
    }

    @Test
    void findByDate_returns404_whenNotFound() throws Exception {
        when(service.findByDate(DATE)).thenThrow(new NotFoundException("not found"));

        mockMvc.perform(get("/api/dailybalances/2026-04-14"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void findAll_returns200_withPageResponse() throws Exception {
        DailyBalancePageResponse.PageableInfo pageable = new DailyBalancePageResponse.PageableInfo(0, 20, 1L, 1);
        DailyBalancePageResponse response = new DailyBalancePageResponse(List.of(buildDTO(BalanceStatus.OPEN)), pageable);
        when(service.findAll(any(), any(), any(), eq(0), eq(20))).thenReturn(response);

        mockMvc.perform(get("/api/dailybalances"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.totalElements").value(1));
    }

    @Test
    void closeBalance_returns200_withClosedStatus() throws Exception {
        when(service.closeBalance(eq(DATE), any(CloseBalanceRequest.class))).thenReturn(buildDTO(BalanceStatus.CLOSED));

        mockMvc.perform(post("/api/dailybalances/2026-04-14/close"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CLOSED"));
    }

    @Test
    void closeBalance_returns409_whenAlreadyClosed() throws Exception {
        when(service.closeBalance(eq(DATE), any(CloseBalanceRequest.class)))
                .thenThrow(new BalanceAlreadyClosedException(DATE));

        mockMvc.perform(post("/api/dailybalances/2026-04-14/close"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("BALANCE_ALREADY_CLOSED"));
    }

    @Test
    void findTransactionsByDate_returns200_withList() throws Exception {
        DailyBalanceTransactionDTO entry = new DailyBalanceTransactionDTO(
                UUID.randomUUID().toString(), "tx-1", "evt-1",
                "CREDIT", "500.00", "BRL", LocalDateTime.now());
        when(service.findTransactionsByDate(DATE)).thenReturn(List.of(entry));

        mockMvc.perform(get("/api/dailybalances/2026-04-14/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].transactionId").value("tx-1"))
                .andExpect(jsonPath("$[0].transactionType").value("CREDIT"))
                .andExpect(jsonPath("$[0].amount").value("500.00"));
    }

    @Test
    void findTransactionsByDate_returns404_whenBalanceNotFound() throws Exception {
        when(service.findTransactionsByDate(DATE)).thenThrow(new NotFoundException("not found"));

        mockMvc.perform(get("/api/dailybalances/2026-04-14/transactions"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void recalculate_returns200_withRecalculateResponse() throws Exception {
        RecalculateResponse.RecalculationDetails details = new RecalculateResponse.RecalculationDetails(
                "0.00", "0.00", "1000.00", "1000.00");
        RecalculateResponse response = new RecalculateResponse(buildDTO(BalanceStatus.OPEN), details);
        when(service.recalculate(DATE)).thenReturn(response);

        mockMvc.perform(post("/api/dailybalances/2026-04-14/recalculate"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.balance.status").value("OPEN"))
                .andExpect(jsonPath("$.recalculationDetails.newClosingBalance").value("1000.00"));
    }
}
