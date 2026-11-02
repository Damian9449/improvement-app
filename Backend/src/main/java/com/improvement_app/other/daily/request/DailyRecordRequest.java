package com.improvement_app.other.daily.request;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.PastOrPresent;

import java.time.LocalDate;

public record DailyRecordRequest(
        // Dzień, którego dotyczy wpis (można uzupełnić wstecz); brak = dzisiaj
        @JsonFormat(pattern = "dd-MM-yyyy") @PastOrPresent LocalDate date,
        boolean health,
        boolean exercise,
        boolean book,
        boolean software
) {
}
