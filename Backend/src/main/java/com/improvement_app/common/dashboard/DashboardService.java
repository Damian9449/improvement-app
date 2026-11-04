package com.improvement_app.common.dashboard;

import jakarta.persistence.EntityManager;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

/**
 * Read-model dla dashboardu: zamiast dokładać metody do repozytoriów czterech modułów
 * (w tym portów heksagonalnego food) robimy tu proste zapytania po nazwach encji.
 */
@Service
@AllArgsConstructor
public class DashboardService {

    private static final int HISTORY_DAYS = 35;

    private final EntityManager entityManager;

    @Transactional(readOnly = true)
    public DashboardSummaryResponse getSummary(Long userId) {
        LocalDate historyFrom = LocalDate.now().minusDays(HISTORY_DAYS - 1L);

        return new DashboardSummaryResponse(
                lastDate("TrainingEntity", userId),
                lastDate("DietSummaryEntity", userId),
                lastDate("WeeklyRecord", userId),
                lastDate("DailyRecord", userId),
                days("TrainingEntity", userId, historyFrom),
                days("DietSummaryEntity", userId, historyFrom),
                days("WeeklyRecord", userId, historyFrom),
                days("DailyRecord", userId, historyFrom)
        );
    }

    // Nazwa encji pochodzi wyłącznie ze stałych powyżej — nie z danych wejściowych
    private LocalDate lastDate(String entityName, Long userId) {
        return entityManager
                .createQuery("SELECT MAX(e.date) FROM " + entityName + " e WHERE e.user.id = :userId", LocalDate.class)
                .setParameter("userId", userId)
                .getSingleResult();
    }

    private List<LocalDate> days(String entityName, Long userId, LocalDate from) {
        return entityManager
                .createQuery("SELECT DISTINCT e.date FROM " + entityName
                        + " e WHERE e.user.id = :userId AND e.date >= :from ORDER BY e.date", LocalDate.class)
                .setParameter("userId", userId)
                .setParameter("from", from)
                .getResultList();
    }
}
