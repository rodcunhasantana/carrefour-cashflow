package com.carrefourbank.transaction.domain.port;

import java.time.LocalDate;

public interface ClosedPeriodRepository {

    boolean isDateClosed(LocalDate date);

    void closeDate(LocalDate date);
}
