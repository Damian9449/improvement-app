package com.improvement_app.home;

import com.improvement_app.food.config.PermitAllSecurityConfig;
import com.improvement_app.food.config.TestContainersConfiguration;
import com.improvement_app.food.infrastructure.repository.DietSummaryRepository;
import com.improvement_app.other.daily.entity.DailyRecord;
import com.improvement_app.other.daily.repository.DailyRepository;
import com.improvement_app.other.daily.request.DailyRecordRequest;
import com.improvement_app.other.weekly.entity.Category;
import com.improvement_app.other.weekly.entity.WeeklyRecord;
import com.improvement_app.other.weekly.repository.WeeklyRepository;
import com.improvement_app.other.weekly.request.WeeklyRecordRequest;
import com.improvement_app.security.entity.UserEntity;
import com.improvement_app.security.repository.UserRepository;
import com.improvement_app.security.services.UserDetailsImpl;
import com.improvement_app.workouts.repository.TrainingEntityRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.List;

import static com.improvement_app.food.data.TestDataFactory.dietSummaryBuilder;
import static com.improvement_app.workouts.data.WorkoutTestDataFactory.training;
import static com.improvement_app.workouts.data.WorkoutTestDataFactory.user;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * E2E DashboardController — daty ostatnich wpisów liczone tylko z danych zalogowanego użytkownika.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestContainersConfiguration.class, PermitAllSecurityConfig.class})
@ActiveProfiles("test")
class DashboardControllerE2ETest {

    @Autowired private MockMvc mockMvc;
    @Autowired private UserRepository userRepository;
    @Autowired private TrainingEntityRepository trainingRepository;
    @Autowired private DietSummaryRepository dietSummaryRepository;
    @Autowired private WeeklyRepository weeklyRepository;
    @Autowired private DailyRepository dailyRepository;

    @BeforeEach
    @AfterEach
    void cleanDb() {
        trainingRepository.deleteAll();
        dietSummaryRepository.deleteAll();
        weeklyRepository.deleteAll();
        dailyRepository.deleteAll();
        userRepository.deleteAll();
    }

    @Test
    @DisplayName("GET /dashboard/summary zwraca najnowsze daty z każdego modułu tylko dla zalogowanego użytkownika")
    void shouldReturnLatestDatesOfCurrentUser() throws Exception {
        UserEntity owner = userRepository.save(user("dash-owner"));
        UserEntity other = userRepository.save(user("dash-other"));

        trainingRepository.save(training(owner, LocalDate.of(2024, 1, 10)));
        trainingRepository.save(training(owner, LocalDate.of(2024, 2, 20)));
        trainingRepository.save(training(other, LocalDate.of(2024, 12, 31)));

        dietSummaryRepository.save(dietSummaryBuilder().date(LocalDate.of(2024, 3, 5)).user(owner).build());
        dietSummaryRepository.save(dietSummaryBuilder().date(LocalDate.of(2024, 12, 31)).user(other).build());

        weeklyRepository.save(weekly(owner, LocalDate.of(2024, 4, 1)));
        weeklyRepository.save(weekly(other, LocalDate.of(2024, 12, 31)));

        dailyRepository.save(daily(owner, LocalDate.of(2024, 5, 1)));
        dailyRepository.save(daily(owner, LocalDate.of(2024, 5, 3)));
        dailyRepository.save(daily(other, LocalDate.of(2024, 12, 31)));

        mockMvc.perform(get("/api/v1/dashboard/summary").with(authentication(authOf(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastTraining").value("2024-02-20"))
                .andExpect(jsonPath("$.lastDietDay").value("2024-03-05"))
                .andExpect(jsonPath("$.lastWeekly").value("2024-04-01"))
                .andExpect(jsonPath("$.lastDaily").value("2024-05-03"));
    }

    @Test
    @DisplayName("GET /dashboard/summary bez wpisów pomija daty (frontend pokazuje 'brak')")
    void shouldOmitDatesWhenNoEntries() throws Exception {
        UserEntity fresh = userRepository.save(user("dash-fresh"));

        mockMvc.perform(get("/api/v1/dashboard/summary").with(authentication(authOf(fresh))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.lastTraining").doesNotExist())
                .andExpect(jsonPath("$.lastDietDay").doesNotExist())
                .andExpect(jsonPath("$.lastWeekly").doesNotExist())
                .andExpect(jsonPath("$.lastDaily").doesNotExist())
                .andExpect(jsonPath("$.dailyDays", hasSize(0)));
    }

    @Test
    @DisplayName("GET /dashboard/summary zwraca bez duplikatów dni z wpisem z ostatnich 35 dni")
    void shouldReturnHistory() throws Exception {
        UserEntity owner = userRepository.save(user("dash-history"));
        UserEntity other = userRepository.save(user("dash-history-other"));
        LocalDate today = LocalDate.now();

        dailyRepository.save(daily(owner, today.minusDays(40)));
        dailyRepository.save(daily(owner, today.minusDays(34)));
        dailyRepository.save(daily(owner, today));
        dailyRepository.save(daily(owner, today));
        dailyRepository.save(daily(other, today.minusDays(1)));

        mockMvc.perform(get("/api/v1/dashboard/summary").with(authentication(authOf(owner))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.dailyDays", contains(today.minusDays(34).toString(), today.toString())));
    }

    private static WeeklyRecord weekly(UserEntity user, LocalDate date) {
        WeeklyRecord weekly = WeeklyRecord.from(new WeeklyRecordRequest("80", Category.Waga), user);
        weekly.setDate(date);
        return weekly;
    }

    private static DailyRecord daily(UserEntity user, LocalDate date) {
        return DailyRecord.from(new DailyRecordRequest(date, true, false, false, false), user);
    }

    private Authentication authOf(UserEntity user) {
        UserDetailsImpl principal = new UserDetailsImpl(
                user.getId(), user.getUsername(), user.getEmail(),
                user.getPassword(), List.of(), true
        );
        return new UsernamePasswordAuthenticationToken(principal, null, List.of());
    }
}
