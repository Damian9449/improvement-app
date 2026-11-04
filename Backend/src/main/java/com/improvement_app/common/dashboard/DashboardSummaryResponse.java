package com.improvement_app.common.dashboard;

import java.time.LocalDate;
import java.util.List;

/**
 * Dane panelu głównego zalogowanego użytkownika. Daty w ISO (yyyy-MM-dd), żeby frontend mógł liczyć
 * zaległości; brak wpisów = pole last* pominięte, listy puste.
 * Pola *Days to dni z wpisem z ostatnich 35 dni.
 */
public record DashboardSummaryResponse(
        LocalDate lastTraining,
        LocalDate lastDietDay,
        LocalDate lastWeekly,
        LocalDate lastDaily,
        List<LocalDate> trainingDays,
        List<LocalDate> dietDays,
        List<LocalDate> weeklyDays,
        List<LocalDate> dailyDays
) {
}
