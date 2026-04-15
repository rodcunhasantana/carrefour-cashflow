package com.carrefourbank.transaction.infrastructure.web;

import com.carrefourbank.common.domain.TransactionType;
import com.carrefourbank.common.exception.NotFoundException;
import com.carrefourbank.transaction.application.command.TransactionCreateCommand;
import com.carrefourbank.transaction.application.dto.ReportStatusResponse;
import com.carrefourbank.transaction.application.dto.TransactionDTO;
import com.carrefourbank.transaction.application.dto.TransactionPageResponse;
import com.carrefourbank.transaction.application.mapper.TransactionMapper;
import com.carrefourbank.transaction.application.port.TransactionService;
import com.carrefourbank.transaction.domain.exception.AlreadyReversedException;
import com.carrefourbank.transaction.domain.model.TransactionStatus;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.format.support.DefaultFormattingConversionService;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.when;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import org.junit.jupiter.api.Tag;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@Tag("unit")
@Tag("web")
@Tag("FR-01")
@Tag("FR-02")
@Tag("FR-03")
@Tag("FR-04")
@ExtendWith(MockitoExtension.class)
class TransactionControllerTest {

    @Mock
    private TransactionService service;

    @Mock
    private TransactionMapper mapper;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        ObjectMapper objectMapper = new ObjectMapper()
                .registerModule(new JavaTimeModule())
                .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

        MappingJackson2HttpMessageConverter converter =
                new MappingJackson2HttpMessageConverter(objectMapper);

        TransactionController controller = new TransactionController(service, mapper);

        mockMvc = MockMvcBuilders.standaloneSetup(controller)
                .setControllerAdvice(new GlobalExceptionHandler())
                .setMessageConverters(converter)
                .setConversionService(new DefaultFormattingConversionService())
                .build();
    }

    private TransactionDTO sampleDTO() {
        return new TransactionDTO(
                UUID.randomUUID().toString(),
                TransactionType.CREDIT,
                "100.00",
                "BRL",
                LocalDate.of(2026, 4, 14),
                "Test transaction",
                LocalDateTime.of(2026, 4, 14, 10, 0, 0),
                TransactionStatus.PENDING,
                false,
                null);
    }

    @Test
    void POST_transactions_returns_201() throws Exception {
        TransactionDTO dto = sampleDTO();
        when(mapper.toCommand(any())).thenReturn(
                new TransactionCreateCommand(
                        TransactionType.CREDIT,
                        new BigDecimal("100.00"),
                        LocalDate.of(2026, 4, 14),
                        "Test transaction"));
        when(service.create(any())).thenReturn(dto);

        mockMvc.perform(post("/api/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"type":"CREDIT","amount":"100.00","date":"2026-04-14","description":"Test transaction"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.type").value("CREDIT"));
    }

    @Test
    void POST_transactions_returns_400_on_missing_fields() throws Exception {
        mockMvc.perform(post("/api/transactions")
                        .contentType(APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void GET_transaction_by_id_returns_200() throws Exception {
        TransactionDTO dto = sampleDTO();
        when(service.findById(any())).thenReturn(dto);

        mockMvc.perform(get("/api/transactions/{id}", UUID.randomUUID()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.type").value("CREDIT"));
    }

    @Test
    void GET_transaction_by_id_returns_404_when_not_found() throws Exception {
        when(service.findById(any())).thenThrow(new NotFoundException("Transaction not found"));

        mockMvc.perform(get("/api/transactions/{id}", UUID.randomUUID()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("RESOURCE_NOT_FOUND"));
    }

    @Test
    void GET_transactions_returns_200_with_page() throws Exception {
        TransactionPageResponse page = new TransactionPageResponse(
                List.of(sampleDTO()),
                new TransactionPageResponse.PageableInfo(0, 20, 1, 1));
        when(service.findAll(any(), any(), any(), anyInt(), anyInt())).thenReturn(page);

        mockMvc.perform(get("/api/transactions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.pageable.totalElements").value(1));
    }

    @Test
    void POST_reverse_returns_200() throws Exception {
        TransactionDTO dto = new TransactionDTO(
                UUID.randomUUID().toString(),
                TransactionType.CREDIT,
                "-100.00",
                "BRL",
                LocalDate.of(2026, 4, 14),
                "Reversal: Test - Reason: Wrong entry",
                LocalDateTime.of(2026, 4, 14, 10, 0, 0),
                TransactionStatus.COMPLETED,
                true,
                UUID.randomUUID().toString());
        when(service.reverse(any(), any())).thenReturn(dto);

        mockMvc.perform(post("/api/transactions/{id}/reverse", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reason":"Wrong entry"}
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.reversal").value(true));
    }

    @Test
    void POST_reverse_returns_409_when_already_reversed() throws Exception {
        when(service.reverse(any(), any()))
                .thenThrow(new AlreadyReversedException(UUID.randomUUID().toString()));

        mockMvc.perform(post("/api/transactions/{id}/reverse", UUID.randomUUID())
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reason":"Try again"}
                                """))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("ALREADY_REVERSED"));
    }

    @Test
    void POST_reports_returns_202() throws Exception {
        ReportStatusResponse response = new ReportStatusResponse(
                UUID.randomUUID().toString(),
                "PROCESSING",
                "/api/transactions/reports/some-id",
                LocalDateTime.of(2026, 4, 14, 10, 5, 0));
        when(service.generateReport(any())).thenReturn(response);

        mockMvc.perform(post("/api/transactions/reports")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"reportType":"DAILY","startDate":"2026-04-01","endDate":"2026-04-14"}
                                """))
                .andExpect(status().isAccepted())
                .andExpect(jsonPath("$.status").value("PROCESSING"));
    }
}
