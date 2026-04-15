package com.carrefourbank.dailybalance.application.mapper;

import com.carrefourbank.dailybalance.application.dto.DailyBalanceDTO;
import com.carrefourbank.dailybalance.domain.model.DailyBalance;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.math.RoundingMode;

@Component
public class DailyBalanceMapper {

    public DailyBalanceDTO toDTO(DailyBalance balance) {
        return new DailyBalanceDTO(
                balance.id().toString(),
                balance.date(),
                format(balance.openingBalance().amount()),
                format(balance.totalCredits().amount()),
                format(balance.totalDebits().amount()),
                format(balance.closingBalance().amount()),
                balance.openingBalance().currency().name(),
                balance.status(),
                balance.closedAt(),
                balance.createdAt(),
                balance.updatedAt());
    }

    private String format(BigDecimal amount) {
        return amount.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
